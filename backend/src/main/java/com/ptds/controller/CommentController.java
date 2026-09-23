package com.ptds.controller;

import com.ptds.dto.CommentRequest;
import com.ptds.dto.CommentResponse;
import com.ptds.dto.PageResponse;
import com.ptds.security.UserPrincipal;
import com.ptds.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/files/{fileId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Threaded comments on a file")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "List comments for a file")
    public ResponseEntity<PageResponse<CommentResponse>> list(
            @PathVariable UUID fileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(commentService.list(fileId, pageable));
    }

    @PostMapping
    @Operation(summary = "Add a comment (or reply) to a file")
    public ResponseEntity<CommentResponse> add(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable UUID fileId,
                                                @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.add(fileId, principal.getId(), request));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment (author or admin)")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable UUID fileId,
                                        @PathVariable UUID commentId) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        commentService.delete(commentId, principal.getId(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
