package com.ptds.repository;

import com.ptds.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByFileIdAndUserId(UUID fileId, UUID userId);

    @Query("select avg(r.score) from Rating r where r.file.id = :fileId")
    Double averageScoreForFile(UUID fileId);

    long countByFileId(UUID fileId);
}
