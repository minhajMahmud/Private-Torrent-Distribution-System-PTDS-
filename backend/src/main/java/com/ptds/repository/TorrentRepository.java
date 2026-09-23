package com.ptds.repository;

import com.ptds.entity.Torrent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TorrentRepository extends JpaRepository<Torrent, UUID> {
    Optional<Torrent> findByFileId(UUID fileId);
}
