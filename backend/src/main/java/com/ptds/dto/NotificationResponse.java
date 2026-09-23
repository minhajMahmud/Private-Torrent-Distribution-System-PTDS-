package com.ptds.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String message;
    private boolean read;
    private OffsetDateTime createdAt;
}
