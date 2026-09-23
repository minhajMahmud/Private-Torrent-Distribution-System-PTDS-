package com.ptds.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemSettingsResponse {
    private String storageMode;
    private long maxUploadSizeBytes;
    private String frontendBaseUrl;
    private String[] allowedFileExtensions;
    private boolean autoApproveUploads;
}
