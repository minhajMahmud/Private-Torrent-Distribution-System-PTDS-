package com.ptds.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long totalFiles;
    private long pendingModeration;
    private long approvedFiles;
    private long rejectedFiles;
    private long totalCategories;
    private long downloadsLast7Days;
    private long downloadsLast30Days;
    private long totalDownloadsAllTime;
    private long lockedUsers;
}
