package com.ptds.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModerationRequest {

    /** APPROVE or REJECT */
    private String decision;

    @Size(max = 500)
    private String rejectionReason;
}
