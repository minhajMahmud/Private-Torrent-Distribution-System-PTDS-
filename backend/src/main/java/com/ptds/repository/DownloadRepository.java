package com.ptds.repository;

import com.ptds.entity.Download;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface DownloadRepository extends JpaRepository<Download, UUID> {
    Page<Download> findByUserIdOrderByDownloadedAtDesc(UUID userId, Pageable pageable);
    long countByDownloadedAtAfter(OffsetDateTime since);
}
