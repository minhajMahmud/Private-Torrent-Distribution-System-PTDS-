package com.ptds.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private boolean emailVerified;
    private Set<String> roles;
    private OffsetDateTime createdAt;
}
