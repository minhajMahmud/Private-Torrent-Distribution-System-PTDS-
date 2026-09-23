package com.ptds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentRequest {

    @NotBlank(message = "Comment content is required")
    @Size(max = 2000)
    private String content;

    private UUID parentId;
}
