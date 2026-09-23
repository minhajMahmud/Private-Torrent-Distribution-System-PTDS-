package com.ptds.controller;

import com.ptds.dto.FileResponse;
import com.ptds.dto.PageResponse;
import com.ptds.security.UserPrincipal;
import com.ptds.service.FavoriteService;
import com.ptds.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "A user's favorited files")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final FileService fileService;

    @GetMapping
    @Operation(summary = "List the current user's favorite files")
    public ResponseEntity<PageResponse<FileResponse>> list(@AuthenticationPrincipal UserPrincipal principal,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(favoriteService.list(principal.getId(), pageable,
                file -> fileService.toPublicResponse(file, principal.getId())));
    }

    @PutMapping("/{fileId}")
    @Operation(summary = "Add a file to favorites")
    public ResponseEntity<Void> add(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID fileId) {
        favoriteService.add(principal.getId(), fileId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Remove a file from favorites")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID fileId) {
        favoriteService.remove(principal.getId(), fileId);
        return ResponseEntity.noContent().build();
    }
}
