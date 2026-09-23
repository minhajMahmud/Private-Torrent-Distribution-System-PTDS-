package com.ptds.repository;

import com.ptds.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID>, JpaSpecificationExecutor<FileEntity> {
    long countByStatus(FileEntity.FileStatus status);
    long countByUploaderId(UUID uploaderId);
}
