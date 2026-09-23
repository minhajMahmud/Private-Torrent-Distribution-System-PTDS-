package com.ptds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Bound from multipart form fields (title, description, categoryId, tags)
 * alongside the uploaded MultipartFile itself.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileUploadRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    private Long categoryId;

    /** Comma-separated tag names, e.g. "os,linux,iso" */
    private String tags;
}
