package com.ptds.service;

import com.ptds.config.StorageProperties;
import com.ptds.config.TrackerProperties;
import com.ptds.dto.TorrentResponse;
import com.ptds.entity.FileEntity;
import com.ptds.entity.Torrent;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.FileRepository;
import com.ptds.repository.TorrentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TorrentServiceTest {

    @Mock private TorrentRepository torrentRepository;
    @Mock private FileRepository fileRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private StorageProperties storageProperties;
    @Mock private TrackerProperties trackerProperties;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private TorrentService torrentService;

    private Torrent existingTorrent(UUID fileId) {
        FileEntity file = FileEntity.builder().id(fileId).build();
        return Torrent.builder()
                .id(UUID.randomUUID())
                .file(file)
                .infoHash("abc123")
                .magnetUri("magnet:?xt=urn:btih:abc123")
                .pieceLength(262144)
                .trackerUrls(List.of("udp://localhost:6969/announce"))
                .healthStatus(Torrent.HealthStatus.UNKNOWN)
                .build();
    }

    @Test
    void applyScrapeResult_marksDeadWhenNoSeeders() {
        UUID fileId = UUID.randomUUID();
        Torrent torrent = existingTorrent(fileId);
        when(torrentRepository.findByFileId(fileId)).thenReturn(Optional.of(torrent));
        when(torrentRepository.save(any(Torrent.class))).thenAnswer(inv -> inv.getArgument(0));

        TorrentResponse response = torrentService.applyScrapeResult(fileId, 0, 5, 10);

        assertThat(response.getHealthStatus()).isEqualTo("DEAD");
        assertThat(response.getSeeders()).isZero();
    }

    @Test
    void applyScrapeResult_marksLowSeedsUnderThreshold() {
        UUID fileId = UUID.randomUUID();
        Torrent torrent = existingTorrent(fileId);
        when(torrentRepository.findByFileId(fileId)).thenReturn(Optional.of(torrent));
        when(torrentRepository.save(any(Torrent.class))).thenAnswer(inv -> inv.getArgument(0));

        TorrentResponse response = torrentService.applyScrapeResult(fileId, 2, 1, 0);

        assertThat(response.getHealthStatus()).isEqualTo("LOW_SEEDS");
    }

    @Test
    void applyScrapeResult_marksHealthyAtThreeOrMoreSeeders() {
        UUID fileId = UUID.randomUUID();
        Torrent torrent = existingTorrent(fileId);
        when(torrentRepository.findByFileId(fileId)).thenReturn(Optional.of(torrent));
        when(torrentRepository.save(any(Torrent.class))).thenAnswer(inv -> inv.getArgument(0));

        TorrentResponse response = torrentService.applyScrapeResult(fileId, 3, 0, 0);

        assertThat(response.getHealthStatus()).isEqualTo("HEALTHY");
    }

    @Test
    void generate_rejectsNonApprovedFiles() {
        UUID fileId = UUID.randomUUID();
        FileEntity pending = FileEntity.builder().id(fileId).status(FileEntity.FileStatus.PENDING).build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> torrentService.generate(fileId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getByFileId_throwsWhenNoTorrentGenerated() {
        UUID fileId = UUID.randomUUID();
        when(torrentRepository.findByFileId(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> torrentService.getByFileId(fileId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
