package com.ptds.controller;

import com.ptds.dto.TorrentResponse;
import com.ptds.security.UserPrincipal;
import com.ptds.service.TorrentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/files/{fileId}/torrent")
@RequiredArgsConstructor
@Tag(name = "Torrents", description = "Torrent metainfo generation, magnet links, and health/scrape stats")
public class TorrentController {

    private final TorrentService torrentService;

    @GetMapping
    @Operation(summary = "Get torrent metadata for a file (if generated)")
    public ResponseEntity<TorrentResponse> get(@PathVariable UUID fileId) {
        return ResponseEntity.ok(torrentService.getByFileId(fileId));
    }

    @PostMapping
    @Operation(summary = "Generate (or fetch existing) torrent metadata + magnet link for an approved file")
    public ResponseEntity<TorrentResponse> generate(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable UUID fileId) {
        return ResponseEntity.ok(torrentService.generate(fileId, principal.getId()));
    }

    @GetMapping("/file")
    @Operation(summary = "Download the raw .torrent file")
    public ResponseEntity<ByteArrayResource> downloadTorrentFile(@PathVariable UUID fileId) {
        byte[] bytes = torrentService.loadTorrentFileBytes(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-bittorrent"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + ".torrent\"")
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/scrape")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually apply a tracker scrape result (admin/ops; normally called by a scheduled job)")
    public ResponseEntity<TorrentResponse> applyScrape(@PathVariable UUID fileId,
                                                        @RequestParam int seeders,
                                                        @RequestParam int leechers,
                                                        @RequestParam(defaultValue = "0") int completed) {
        return ResponseEntity.ok(torrentService.applyScrapeResult(fileId, seeders, leechers, completed));
    }
}
