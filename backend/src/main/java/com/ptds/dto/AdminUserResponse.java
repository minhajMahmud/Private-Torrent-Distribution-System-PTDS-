package com.ptds.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUserResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private boolean emailVerified;
    private boolean enabled;
    private boolean locked;
    private Set<String> roles;
    private long uploadCount;
    private OffsetDateTime createdAt;
}
