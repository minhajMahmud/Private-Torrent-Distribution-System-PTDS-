package com.ptds.service;

import com.ptds.dto.FileResponse;
import com.ptds.dto.PageResponse;
import com.ptds.entity.FileEntity;
import com.ptds.entity.Favorite;
import com.ptds.entity.FavoriteId;
import com.ptds.entity.User;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.FavoriteRepository;
import com.ptds.repository.FileRepository;
import com.ptds.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Transactional
    public void add(UUID userId, UUID fileId) {
        FavoriteId id = new FavoriteId(userId, fileId);
        if (favoriteRepository.existsById(id)) return;

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        FileEntity file = fileRepository.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File not found"));

        favoriteRepository.save(Favorite.builder().id(id).user(user).file(file).build());
    }

    @Transactional
    public void remove(UUID userId, UUID fileId) {
        favoriteRepository.deleteById(new FavoriteId(userId, fileId));
    }

    @Transactional(readOnly = true)
    public PageResponse<FileResponse> list(UUID userId, Pageable pageable, Function<FileEntity, FileResponse> mapper) {
        Page<Favorite> page = favoriteRepository.findByUserId(userId, pageable);
        return PageResponse.of(page.map(f -> mapper.apply(f.getFile())));
    }
}
