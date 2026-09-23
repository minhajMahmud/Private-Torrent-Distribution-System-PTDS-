package com.ptds.service;

import com.ptds.config.StorageProperties;
import com.ptds.config.TrackerProperties;
import com.ptds.dto.TorrentResponse;
import com.ptds.entity.FileEntity;
import com.ptds.entity.Torrent;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.exception.StorageException;
import com.ptds.repository.FileRepository;
import com.ptds.repository.TorrentRepository;
import com.ptds.util.BencodeEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates real, spec-compliant single-file .torrent metainfo (BEP 3) for
 * an already-stored, moderator-approved file, plus its magnet URI. Because
 * PTDS sits behind authenticated access control rather than a public swarm,
 * seeder/leecher/health figures are the values reported back by the tracker
 * scrape (Phase 3 wiring point) rather than fabricated — this service stores
 * whatever the tracker last reported and exposes it as-is.
 */
@Service
@RequiredArgsConstructor
public class TorrentService {

    private final TorrentRepository torrentRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;
    private final TrackerProperties trackerProperties;
    private final AuditLogService auditLogService;

    @Transactional
    public TorrentResponse generate(UUID fileId, UUID requesterId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        if (file.getStatus() != FileEntity.FileStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved files can have a torrent generated");
        }

        Torrent existing = torrentRepository.findByFileId(fileId).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        Path filePath = fileStorageService.resolvePath(file.getStorageKey());
        int pieceLength = trackerProperties.getPieceLengthBytes();
        byte[] pieces = hashPieces(filePath, pieceLength);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", file.getOriginalName());
        info.put("length", file.getSizeBytes());
        info.put("piece length", pieceLength);
        info.put("pieces", pieces);
        info.put("private", 1); // private flag: no DHT/PEX, tracker-only swarm — matches PTDS's authorized-access model

        String infoHash = sha1Hex(BencodeEncoder.encode(info));

        Map<String, Object> metainfo = new LinkedHashMap<>();
        metainfo.put("announce", trackerProperties.getAnnounceUrls().get(0));
        metainfo.put("announce-list", List.of(trackerProperties.getAnnounceUrls()));
        metainfo.put("created by", "PTDS");
        metainfo.put("creation date", OffsetDateTime.now().toEpochSecond());
        metainfo.put("info", info);

        byte[] torrentBytes = BencodeEncoder.encode(metainfo);
        String torrentFileName = infoHash + ".torrent";
        try {
            Path torrentsRoot = Path.of(storageProperties.getLocalPath(), "torrents");
            Files.createDirectories(torrentsRoot);
            Files.write(torrentsRoot.resolve(torrentFileName), torrentBytes);
        } catch (IOException e) {
            throw new StorageException("Failed to write .torrent file", e);
        }

        String magnetUri = buildMagnetUri(infoHash, file.getOriginalName());

        Torrent torrent = Torrent.builder()
                .file(file)
                .infoHash(infoHash)
                .torrentPath("torrents/" + torrentFileName)
                .magnetUri(magnetUri)
                .pieceLength(pieceLength)
                .trackerUrls(trackerProperties.getAnnounceUrls())
                .healthStatus(Torrent.HealthStatus.UNKNOWN)
                .build();
        Torrent saved = torrentRepository.save(torrent);

        auditLogService.log(requesterId, "TORRENT_GENERATED", "Torrent", saved.getId().toString());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TorrentResponse getByFileId(UUID fileId) {
        Torrent torrent = torrentRepository.findByFileId(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("No torrent has been generated for this file yet"));
        return toResponse(torrent);
    }

    public byte[] loadTorrentFileBytes(UUID fileId) {
        Torrent torrent = torrentRepository.findByFileId(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("No torrent has been generated for this file yet"));
        try {
            return Files.readAllBytes(Path.of(storageProperties.getLocalPath(), torrent.getTorrentPath()));
        } catch (IOException e) {
            throw new StorageException("Failed to read .torrent file", e);
        }
    }

    /**
     * Applies a tracker scrape result (seeders/leechers/completed). In a full
     * deployment this is called by a scheduled job that talks to the Chihaya
     * tracker's scrape endpoint; the HTTP call sits behind this single method
     * so the scheduling/HTTP-client wiring can be swapped in later without
     * touching health-status logic.
     */
    @Transactional
    public TorrentResponse applyScrapeResult(UUID fileId, int seeders, int leechers, int completed) {
        Torrent torrent = torrentRepository.findByFileId(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("No torrent has been generated for this file yet"));
        torrent.setSeeders(seeders);
        torrent.setLeechers(leechers);
        torrent.setCompleted(completed);
        torrent.setLastScrapedAt(OffsetDateTime.now());
        torrent.setHealthStatus(computeHealth(seeders));
        return toResponse(torrentRepository.save(torrent));
    }

    private Torrent.HealthStatus computeHealth(int seeders) {
        if (seeders <= 0) return Torrent.HealthStatus.DEAD;
        if (seeders < 3) return Torrent.HealthStatus.LOW_SEEDS;
        return Torrent.HealthStatus.HEALTHY;
    }

    private byte[] hashPieces(Path filePath, int pieceLength) {
        try (var in = Files.newInputStream(filePath)) {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            java.io.ByteArrayOutputStream piecesOut = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[pieceLength];
            int read;
            while ((read = in.readNBytes(buffer, 0, pieceLength)) > 0) {
                sha1.reset();
                sha1.update(buffer, 0, read);
                piecesOut.writeBytes(sha1.digest());
            }
            return piecesOut.toByteArray();
        } catch (IOException e) {
            throw new StorageException("Failed to read file for piece hashing", e);
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("SHA-1 algorithm unavailable", e);
        }
    }

    private String sha1Hex(byte[] data) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(data);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("SHA-1 algorithm unavailable", e);
        }
    }

    private String buildMagnetUri(String infoHashHex, String displayName) {
        StringBuilder sb = new StringBuilder("magnet:?xt=urn:btih:").append(infoHashHex);
        sb.append("&dn=").append(URLEncoder.encode(displayName, StandardCharsets.UTF_8));
        for (String tracker : trackerProperties.getAnnounceUrls()) {
            sb.append("&tr=").append(URLEncoder.encode(tracker, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private TorrentResponse toResponse(Torrent torrent) {
        return TorrentResponse.builder()
                .id(torrent.getId())
                .fileId(torrent.getFile().getId())
                .infoHash(torrent.getInfoHash())
                .magnetUri(torrent.getMagnetUri())
                .pieceLength(torrent.getPieceLength())
                .trackerUrls(torrent.getTrackerUrls())
                .seeders(torrent.getSeeders())
                .leechers(torrent.getLeechers())
                .completed(torrent.getCompleted())
                .healthStatus(torrent.getHealthStatus().name())
                .lastScrapedAt(torrent.getLastScrapedAt())
                .createdAt(torrent.getCreatedAt())
                .build();
    }
}
