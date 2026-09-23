package com.ptds.controller;

import com.ptds.dto.RatingRequest;
import com.ptds.dto.RatingSummaryResponse;
import com.ptds.security.UserPrincipal;
import com.ptds.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/files/{fileId}/ratings")
@RequiredArgsConstructor
@Tag(name = "Ratings", description = "1-5 star ratings on a file")
public class RatingController {

    private final RatingService ratingService;

    @GetMapping
    @Operation(summary = "Get the rating summary (average, count, my score) for a file")
    public ResponseEntity<RatingSummaryResponse> summary(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable UUID fileId) {
        return ResponseEntity.ok(ratingService.summary(fileId, principal != null ? principal.getId() : null));
    }

    @PutMapping
    @Operation(summary = "Rate a file (creates or updates the caller's own rating)")
    public ResponseEntity<RatingSummaryResponse> rate(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable UUID fileId,
                                                        @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ratingService.rate(fileId, principal.getId(), request.getScore()));
    }
}
