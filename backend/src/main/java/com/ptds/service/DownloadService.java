package com.ptds.service;

import com.ptds.entity.Download;
import com.ptds.entity.FileEntity;
import com.ptds.entity.User;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.DownloadRepository;
import com.ptds.repository.FileRepository;
import com.ptds.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DownloadRepository downloadRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(UUID userId, UUID fileId, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        FileEntity file = fileRepository.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File not found"));
        downloadRepository.save(Download.builder()
                .user(user).file(file).ipAddress(ipAddress).userAgent(userAgent)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<DownloadHistoryItem> history(UUID userId, Pageable pageable) {
        return downloadRepository.findByUserIdOrderByDownloadedAtDesc(userId, pageable)
                .map(d -> new DownloadHistoryItem(
                        d.getId(), d.getFile().getId(), d.getFile().getTitle(), d.getDownloadedAt()));
    }

    @Transactional(readOnly = true)
    public long countSince(OffsetDateTime since) {
        return downloadRepository.countByDownloadedAtAfter(since);
    }

    @Getter
    public static class DownloadHistoryItem {
        private final UUID id;
        private final UUID fileId;
        private final String fileTitle;
        private final OffsetDateTime downloadedAt;

        public DownloadHistoryItem(UUID id, UUID fileId, String fileTitle, OffsetDateTime downloadedAt) {
            this.id = id;
            this.fileId = fileId;
            this.fileTitle = fileTitle;
            this.downloadedAt = downloadedAt;
        }
    }
}
