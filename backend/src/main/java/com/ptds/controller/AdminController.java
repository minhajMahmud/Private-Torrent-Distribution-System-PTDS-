package com.ptds.controller;

import com.ptds.dto.*;
import com.ptds.config.StorageProperties;
import com.ptds.config.AppCorsProperties;
import com.ptds.security.UserPrincipal;
import com.ptds.service.AdminService;
import com.ptds.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * All endpoints here require ROLE_ADMIN — enforced both by the class-level
 * @PreAuthorize (defense in depth) and by SecurityConfig's "/api/admin/**"
 * matcher.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Dashboard, user management, file moderation, and system settings")
public class AdminController {

    private final AdminService adminService;
    private final FileService fileService;
    private final StorageProperties storageProperties;
    private final AppCorsProperties corsProperties;

    @GetMapping("/dashboard")
    @Operation(summary = "Aggregate platform statistics")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        return ResponseEntity.ok(adminService.dashboard());
    }

    @GetMapping("/users")
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<Page<AdminUserResponse>> listUsers(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.listUsers(PageRequest.of(page, size)));
    }

    @PatchMapping("/users/{userId}")
    @Operation(summary = "Lock/unlock, enable/disable, or change a user's role")
    public ResponseEntity<AdminUserResponse> updateUser(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable UUID userId,
                                                         @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateUser(userId, request, principal.getId()));
    }

    @GetMapping("/moderation-queue")
    @Operation(summary = "List files pending moderation")
    public ResponseEntity<PageResponse<FileResponse>> moderationQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(fileService.search(null, null, null, "PENDING", null,
                principal.getId(), true, pageable));
    }

    @PostMapping("/files/{fileId}/moderate")
    @Operation(summary = "Approve or reject a pending file")
    public ResponseEntity<FileResponse> moderate(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable UUID fileId,
                                                  @Valid @RequestBody ModerationRequest request) {
        boolean approve = "APPROVE".equalsIgnoreCase(request.getDecision());
        return ResponseEntity.ok(fileService.moderate(fileId, principal.getId(), approve, request.getRejectionReason()));
    }

    @GetMapping("/settings")
    @Operation(summary = "View active system settings (read-only in this delivery)")
    public ResponseEntity<SystemSettingsResponse> settings() {
        return ResponseEntity.ok(SystemSettingsResponse.builder()
                .storageMode(storageProperties.getMode())
                .maxUploadSizeBytes(storageProperties.getMaxSizeBytes())
                .frontendBaseUrl(corsProperties.getAllowedOrigins())
                .allowedFileExtensions(storageProperties.getAllowedExtensions().toArray(new String[0]))
                .autoApproveUploads(storageProperties.isAutoApproveUploads())
                .build());
    }
}
