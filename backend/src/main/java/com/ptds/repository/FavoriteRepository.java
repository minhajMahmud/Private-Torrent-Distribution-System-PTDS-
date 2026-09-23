package com.ptds.repository;

import com.ptds.entity.Favorite;
import com.ptds.entity.FavoriteId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    Page<Favorite> findByUserId(UUID userId, Pageable pageable);
    boolean existsById(FavoriteId id);
}
