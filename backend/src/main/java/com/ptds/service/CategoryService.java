package com.ptds.service;

import com.ptds.dto.CategoryRequest;
import com.ptds.dto.CategoryResponse;
import com.ptds.entity.Category;
import com.ptds.exception.DuplicateResourceException;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.CategoryRepository;
import com.ptds.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final FileRepository fileRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }
        String slug = slugify(request.getName());
        int suffix = 1;
        String uniqueSlug = slug;
        while (categoryRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = slug + "-" + (++suffix);
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(uniqueSlug)
                .description(request.getDescription())
                .parent(parent)
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getParentId() != null) {
            category.setParent(categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found")));
        } else {
            category.setParent(null);
        }
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found");
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponse toResponse(Category category) {
        long fileCount = fileRepository.count((root, query, cb) ->
                cb.equal(root.get("category").get("id"), category.getId()));
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .fileCount(fileCount)
                .build();
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = Pattern.compile("[^a-zA-Z0-9]+")
                .matcher(normalized.toLowerCase(Locale.ROOT))
                .replaceAll("-");
        return slug.replaceAll("^-|-$", "");
    }
}
