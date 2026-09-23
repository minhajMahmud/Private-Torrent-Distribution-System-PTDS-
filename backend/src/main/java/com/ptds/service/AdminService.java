package com.ptds.service;

import com.ptds.dto.AdminDashboardResponse;
import com.ptds.dto.AdminUserResponse;
import com.ptds.dto.AdminUserUpdateRequest;
import com.ptds.entity.FileEntity;
import com.ptds.entity.Role;
import com.ptds.entity.User;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final CategoryRepository categoryRepository;
    private final DownloadRepository downloadRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalFiles(fileRepository.count())
                .pendingModeration(fileRepository.countByStatus(FileEntity.FileStatus.PENDING))
                .approvedFiles(fileRepository.countByStatus(FileEntity.FileStatus.APPROVED))
                .rejectedFiles(fileRepository.countByStatus(FileEntity.FileStatus.REJECTED))
                .totalCategories(categoryRepository.count())
                .downloadsLast7Days(downloadRepository.countByDownloadedAtAfter(OffsetDateTime.now().minusDays(7)))
                .downloadsLast30Days(downloadRepository.countByDownloadedAtAfter(OffsetDateTime.now().minusDays(30)))
                .totalDownloadsAllTime(downloadRepository.count())
                .lockedUsers(userRepository.countByLockedTrue())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public AdminUserResponse updateUser(UUID userId, AdminUserUpdateRequest request, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getLocked() != null) user.setLocked(request.getLocked());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());
        if (request.getRole() != null && !request.getRole().isBlank()) {
            Role role = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));
            user.setRoles(Set.of(role));
        }

        User saved = userRepository.save(user);
        auditLogService.log(adminId, "ADMIN_USER_UPDATE", "User", userId.toString());
        return toResponse(saved);
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .locked(user.isLocked())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .uploadCount(fileRepository.countByUploaderId(user.getId()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
