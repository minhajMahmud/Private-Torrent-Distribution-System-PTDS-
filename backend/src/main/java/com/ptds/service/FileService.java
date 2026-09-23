package com.ptds.service;

import com.ptds.config.StorageProperties;
import com.ptds.dto.FileResponse;
import com.ptds.dto.FileUploadRequest;
import com.ptds.dto.PageResponse;
import com.ptds.entity.*;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final TorrentRepository torrentRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public FileResponse upload(UUID uploaderId, FileUploadRequest request, MultipartFile multipartFile) {
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        FileStorageService.StoredFile stored = fileStorageService.store(multipartFile);

        FileEntity file = FileEntity.builder()
                .uploader(uploader)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .originalName(stored.originalName())
                .storageKey(stored.storageKey())
                .sizeBytes(stored.sizeBytes())
                .mimeType(stored.mimeType())
                .checksumSha256(stored.checksumSha256())
                .status(storageProperties.isAutoApproveUploads()
                        ? FileEntity.FileStatus.APPROVED
                        : FileEntity.FileStatus.PENDING)
                .tags(resolveTags(request.getTags()))
                .build();

        FileEntity saved = fileRepository.save(file);
        auditLogService.log(uploaderId, "FILE_UPLOAD", "File", saved.getId().toString());
        return toResponse(saved, uploaderId);
    }

    @Transactional(readOnly = true)
    public PageResponse<FileResponse> search(String keyword, Long categoryId, String tag,
                                              String status, UUID uploaderId, UUID viewerId,
                                              boolean viewerIsAdmin, Pageable pageable) {
        Specification<FileEntity> spec = buildSpecification(keyword, categoryId, tag, status, uploaderId, viewerId, viewerIsAdmin);
        Page<FileEntity> page = fileRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(f -> toResponse(f, viewerId)));
    }

    @Transactional(readOnly = true)
    public FileResponse getById(UUID fileId, UUID viewerId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        return toResponse(file, viewerId);
    }

    @Transactional
    public void delete(UUID fileId, UUID requesterId, boolean requesterIsAdmin) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        if (!requesterIsAdmin && !file.getUploader().getId().equals(requesterId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your file");
        }
        fileRepository.delete(file);
        auditLogService.log(requesterId, "FILE_DELETE", "File", fileId.toString());
    }

    @Transactional
    public FileResponse moderate(UUID fileId, UUID adminId, boolean approve, String rejectionReason) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        file.setStatus(approve ? FileEntity.FileStatus.APPROVED : FileEntity.FileStatus.REJECTED);
        file.setModeratedBy(adminId);
        file.setModeratedAt(java.time.OffsetDateTime.now());
        file.setRejectionReason(approve ? null : rejectionReason);
        FileEntity saved = fileRepository.save(file);

        notificationService.notify(file.getUploader().getId(),
                approve ? Notification.NotificationType.FILE_APPROVED.name() : Notification.NotificationType.FILE_REJECTED.name(),
                approve ? "Your file was approved" : "Your file was rejected",
                approve ? ("\"" + file.getTitle() + "\" is now live.") : ("\"" + file.getTitle() + "\": " + rejectionReason));

        auditLogService.log(adminId, approve ? "FILE_APPROVE" : "FILE_REJECT", "File", fileId.toString());
        return toResponse(saved, adminId);
    }

    /** Records a download event and returns the storage key + original filename to stream back. */
    @Transactional
    public FileEntity registerDownload(UUID fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        file.setDownloadCount(file.getDownloadCount() + 1);
        return fileRepository.save(file);
    }

    private Set<Tag> resolveTags(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(name -> tagRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name.toLowerCase(Locale.ROOT)).build())))
                .collect(Collectors.toSet());
    }

    private Specification<FileEntity> buildSpecification(String keyword, Long categoryId, String tag,
                                                           String status, UUID uploaderId, UUID viewerId,
                                                           boolean viewerIsAdmin) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (tag != null && !tag.isBlank()) {
                query.distinct(true);
                predicates.add(cb.equal(cb.lower(root.join("tags").get("name")), tag.toLowerCase(Locale.ROOT)));
            }
            if (uploaderId != null) {
                predicates.add(cb.equal(root.get("uploader").get("id"), uploaderId));
            }

            // Non-admins only ever see APPROVED files, unless they're looking at their own uploads.
            if (!viewerIsAdmin) {
                if (uploaderId != null && uploaderId.equals(viewerId)) {
                    if (status != null && !status.isBlank()) {
                        predicates.add(cb.equal(root.get("status"), FileEntity.FileStatus.valueOf(status.toUpperCase(Locale.ROOT))));
                    }
                } else {
                    predicates.add(cb.equal(root.get("status"), FileEntity.FileStatus.APPROVED));
                }
            } else if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), FileEntity.FileStatus.valueOf(status.toUpperCase(Locale.ROOT))));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Public entry point so other services (e.g. favorites listing) can reuse the same mapping. */
    public FileResponse toPublicResponse(FileEntity file, UUID viewerId) {
        return toResponse(file, viewerId);
    }

    private FileResponse toResponse(FileEntity file, UUID viewerId) {
        Double avg = ratingRepository.averageScoreForFile(file.getId());
        long ratingCount = ratingRepository.countByFileId(file.getId());
        long commentCount = commentRepository.countByFileId(file.getId());
        boolean favorited = viewerId != null && favoriteRepository.existsById(new FavoriteId(viewerId, file.getId()));
        boolean torrentAvailable = torrentRepository.findByFileId(file.getId()).isPresent();

        return FileResponse.builder()
                .id(file.getId())
                .title(file.getTitle())
                .description(file.getDescription())
                .categoryId(file.getCategory() != null ? file.getCategory().getId() : null)
                .categoryName(file.getCategory() != null ? file.getCategory().getName() : null)
                .tags(file.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                .originalName(file.getOriginalName())
                .sizeBytes(file.getSizeBytes())
                .mimeType(file.getMimeType())
                .checksumSha256(file.getChecksumSha256())
                .status(file.getStatus().name())
                .rejectionReason(file.getRejectionReason())
                .downloadCount(file.getDownloadCount())
                .uploaderId(file.getUploader().getId())
                .uploaderUsername(file.getUploader().getUsername())
                .averageRating(avg)
                .ratingCount(ratingCount)
                .commentCount(commentCount)
                .favorited(favorited)
                .torrentAvailable(torrentAvailable)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
