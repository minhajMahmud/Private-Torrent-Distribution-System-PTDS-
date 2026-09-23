package com.ptds.controller;

import com.ptds.security.UserPrincipal;
import com.ptds.service.DownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/downloads")
@RequiredArgsConstructor
@Tag(name = "Download History", description = "A user's own download history")
public class DownloadController {

    private final DownloadService downloadService;

    @GetMapping("/history")
    @Operation(summary = "List the current user's download history")
    public ResponseEntity<Page<DownloadService.DownloadHistoryItem>> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(downloadService.history(principal.getId(), pageable));
    }
}
