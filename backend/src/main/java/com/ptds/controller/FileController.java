package com.ptds.controller;

import com.ptds.dto.FileResponse;
import com.ptds.dto.FileUploadRequest;
import com.ptds.dto.PageResponse;
import com.ptds.entity.FileEntity;
import com.ptds.security.UserPrincipal;
import com.ptds.service.DownloadService;
import com.ptds.service.FileService;
import com.ptds.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Upload, search, download and manage authorized files")
public class FileController {

    private final FileService fileService;
    private final FileStorageService fileStorageService;
    private final DownloadService downloadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new file for moderation")
    public ResponseEntity<FileResponse> upload(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @ModelAttribute FileUploadRequest request,
                                                @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileService.upload(principal.getId(), request, file));
    }

    @GetMapping
    @Operation(summary = "Search and list files (approved-only for regular users)")
    public ResponseEntity<PageResponse<FileResponse>> search(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID uploaderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        UUID viewerId = principal != null ? principal.getId() : null;

        Sort sort = Sort.by(direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(fileService.search(keyword, categoryId, tag, status, uploaderId, viewerId, isAdmin, pageable));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file details")
    public ResponseEntity<FileResponse> getById(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable UUID fileId) {
        return ResponseEntity.ok(fileService.getById(fileId, principal != null ? principal.getId() : null));
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "Download the underlying file and record the download event")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable UUID fileId,
                                              HttpServletRequest request) {
        FileResponse meta = fileService.getById(fileId, principal.getId());
        FileEntity updated = fileService.registerDownload(fileId);
        downloadService.record(principal.getId(), fileId, request.getRemoteAddr(), request.getHeader("User-Agent"));

        Resource resource = fileStorageService.load(updated.getStorageKey());
        String encodedName = URLEncoder.encode(meta.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(meta.getMimeType() != null ? MediaType.parseMediaType(meta.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete a file (owner or admin)")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID fileId) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        fileService.delete(fileId, principal.getId(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
