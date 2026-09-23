package com.ptds.service;

import com.ptds.dto.RatingSummaryResponse;
import com.ptds.entity.FileEntity;
import com.ptds.entity.Rating;
import com.ptds.entity.User;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.FileRepository;
import com.ptds.repository.RatingRepository;
import com.ptds.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Transactional
    public RatingSummaryResponse rate(UUID fileId, UUID userId, short score) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Rating rating = ratingRepository.findByFileIdAndUserId(fileId, userId)
                .orElseGet(() -> Rating.builder().file(file).user(user).build());
        rating.setScore(score);
        ratingRepository.save(rating);

        return summary(fileId, userId);
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse summary(UUID fileId, UUID viewerId) {
        Double avg = ratingRepository.averageScoreForFile(fileId);
        long count = ratingRepository.countByFileId(fileId);
        Short mine = viewerId == null ? null
                : ratingRepository.findByFileIdAndUserId(fileId, viewerId).map(Rating::getScore).orElse(null);
        return RatingSummaryResponse.builder()
                .average(avg == null ? 0.0 : avg)
                .count(count)
                .myScore(mine)
                .build();
    }
}
