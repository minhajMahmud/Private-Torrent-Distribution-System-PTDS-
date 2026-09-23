package com.ptds.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUserUpdateRequest {
    private Boolean locked;
    private Boolean enabled;
    /** Optional: "ROLE_USER" or "ROLE_ADMIN" — replaces the user's role set with this single role. */
    private String role;
}
