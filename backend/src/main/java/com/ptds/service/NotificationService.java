package com.ptds.service;

import com.ptds.dto.NotificationResponse;
import com.ptds.dto.PageResponse;
import com.ptds.entity.Notification;
import com.ptds.entity.User;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.NotificationRepository;
import com.ptds.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /** Runs in its own transaction so a notification failure never rolls back the triggering action. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(UUID userId, String type, String title, String message) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        Notification notification = Notification.builder()
                .user(user).type(type).title(title).message(message).build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).type(n.getType()).title(n.getTitle())
                .message(n.getMessage()).read(n.isRead()).createdAt(n.getCreatedAt())
                .build();
    }
}
