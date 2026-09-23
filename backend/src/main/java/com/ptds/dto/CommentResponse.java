package com.ptds.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentResponse {
    private UUID id;
    private UUID fileId;
    private UUID userId;
    private String username;
    private String avatarUrl;
    private UUID parentId;
    private String content;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
