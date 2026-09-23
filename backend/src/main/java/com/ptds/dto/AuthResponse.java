package com.ptds.dto;

import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private UUID userId;
    private String username;
    private String email;
    private Set<String> roles;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
