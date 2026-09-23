package com.ptds.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.storage")
@Getter @Setter
public class StorageProperties {
    /** local | minio (only local is implemented in this delivery) */
    private String mode = "local";
    private String localPath = "./storage";
    /** Allowed file extensions for uploads, without the dot. */
    private List<String> allowedExtensions = List.of(
            "zip", "tar", "gz", "7z", "iso", "pdf", "epub", "txt", "md",
            "doc", "docx", "ppt", "pptx", "xls", "xlsx", "csv"
    );
    private long maxSizeBytes = 2147483648L; // 2GB
    /** If true, uploads skip the PENDING moderation queue and go straight to APPROVED. */
    private boolean autoApproveUploads = false;
}
