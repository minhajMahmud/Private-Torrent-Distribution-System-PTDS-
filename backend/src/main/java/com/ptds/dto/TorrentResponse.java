package com.ptds.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TorrentResponse {
    private UUID id;
    private UUID fileId;
    private String infoHash;
    private String magnetUri;
    private int pieceLength;
    private List<String> trackerUrls;
    private int seeders;
    private int leechers;
    private int completed;
    private String healthStatus;
    private OffsetDateTime lastScrapedAt;
    private OffsetDateTime createdAt;
}
