package com.ptds.service;

import com.ptds.dto.CommentRequest;
import com.ptds.dto.CommentResponse;
import com.ptds.dto.PageResponse;
import com.ptds.entity.Comment;
import com.ptds.entity.FileEntity;
import com.ptds.entity.Notification;
import com.ptds.entity.User;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.CommentRepository;
import com.ptds.repository.FileRepository;
import com.ptds.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public CommentResponse add(UUID fileId, UUID userId, CommentRequest request) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
        }

        Comment comment = Comment.builder()
                .file(file).user(user).parent(parent).content(request.getContent())
                .build();
        Comment saved = commentRepository.save(comment);

        if (!file.getUploader().getId().equals(userId)) {
            notificationService.notify(file.getUploader().getId(), Notification.NotificationType.NEW_COMMENT.name(),
                    "New comment on your file", user.getUsername() + " commented on \"" + file.getTitle() + "\"");
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(UUID fileId, Pageable pageable) {
        Page<Comment> page = commentRepository.findByFileIdOrderByCreatedAtDesc(fileId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional
    public void delete(UUID commentId, UUID requesterId, boolean requesterIsAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!requesterIsAdmin && !comment.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("Not your comment");
        }
        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .fileId(c.getFile().getId())
                .userId(c.getUser().getId())
                .username(c.getUser().getUsername())
                .avatarUrl(c.getUser().getAvatarUrl())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
