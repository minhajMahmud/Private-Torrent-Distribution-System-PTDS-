package com.ptds.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileResponse {
    private UUID id;
    private String title;
    private String description;
    private String categoryName;
    private Long categoryId;
    private Set<String> tags;
    private String originalName;
    private long sizeBytes;
    private String mimeType;
    private String checksumSha256;
    private String status;
    private String rejectionReason;
    private long downloadCount;
    private UUID uploaderId;
    private String uploaderUsername;
    private Double averageRating;
    private long ratingCount;
    private long commentCount;
    private boolean favorited;
    private boolean torrentAvailable;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
