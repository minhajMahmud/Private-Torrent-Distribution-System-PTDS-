package com.ptds.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "torrents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Torrent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    private FileEntity file;

    @Column(name = "info_hash", nullable = false, unique = true, length = 40)
    private String infoHash;

    @Column(name = "torrent_path", nullable = false, length = 500)
    private String torrentPath;

    @Column(name = "magnet_uri", nullable = false, columnDefinition = "TEXT")
    private String magnetUri;

    @Column(name = "piece_length", nullable = false)
    private int pieceLength;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tracker_urls", nullable = false, columnDefinition = "text[]")
    private List<String> trackerUrls;

    @Column(nullable = false)
    @Builder.Default
    private int seeders = 0;

    @Column(nullable = false)
    @Builder.Default
    private int leechers = 0;

    @Column(nullable = false)
    @Builder.Default
    private int completed = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 20)
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "last_scraped_at")
    private OffsetDateTime lastScrapedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public enum HealthStatus { HEALTHY, LOW_SEEDS, DEAD, UNKNOWN }
}
