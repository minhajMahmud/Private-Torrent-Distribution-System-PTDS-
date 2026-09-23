package com.ptds.service;

import com.ptds.dto.CategoryRequest;
import com.ptds.dto.CategoryResponse;
import com.ptds.entity.Category;
import com.ptds.exception.DuplicateResourceException;
import com.ptds.repository.CategoryRepository;
import com.ptds.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private FileRepository fileRepository;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        lenient().when(fileRepository.count(any())).thenReturn(0L);
    }

    @Test
    void create_generatesUrlSafeSlugFromName() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Research Papers & Datasets")
                .description("Academic material")
                .build();

        when(categoryRepository.existsByName(request.getName())).thenReturn(false);
        when(categoryRepository.existsBySlug(any())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.getSlug()).isEqualTo("research-papers-datasets");
        assertThat(response.getName()).isEqualTo(request.getName());
    }

    @Test
    void create_rejectsDuplicateName() {
        CategoryRequest request = CategoryRequest.builder().name("Software").build();
        when(categoryRepository.existsByName("Software")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_disambiguatesSlugCollisions() {
        CategoryRequest request = CategoryRequest.builder().name("Software").build();
        when(categoryRepository.existsByName("Software")).thenReturn(false);
        when(categoryRepository.existsBySlug("software")).thenReturn(true);
        when(categoryRepository.existsBySlug("software-2")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.getSlug()).isEqualTo("software-2");
    }
}
