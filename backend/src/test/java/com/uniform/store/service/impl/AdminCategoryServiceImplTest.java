package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateCategoryRequest;
import com.uniform.store.dto.request.UpdateCategoryRequest;
import com.uniform.store.dto.response.AdminCategoryDto;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.CategoryTranslation;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.CategoryTranslationRepository;
import com.uniform.store.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryTranslationRepository translationRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private AdminCategoryServiceImpl service;

    private Category category;

    @BeforeEach
    void setup() {
        category = Category.builder().slug("tees").name("T-Shirts").sortOrder(0).isActive(true).build();
        category.setId(1L);
    }

    @Test
    void create_rejectsDuplicateSlug() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setSlug("tees");
        req.setName("T-Shirts");
        when(categoryRepository.existsBySlug("tees")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("slug already exists");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_savesAndPersistsViTranslation() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setSlug("hoodies");
        req.setName("Hoodies");
        req.setNameVi("Áo hoodie");
        when(categoryRepository.existsBySlug("hoodies")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(translationRepository.findByCategoryIdAndLocale(2L, "vi")).thenReturn(Optional.empty());
        when(translationRepository.findByCategoryIdAndLocale(2L, "ja")).thenReturn(Optional.empty());
        when(productRepository.countByCategoryIdAndDeletedAtIsNull(2L)).thenReturn(0L);

        AdminCategoryDto out = service.create(req);

        assertThat(out).isNotNull();
        verify(translationRepository).save(any(CategoryTranslation.class));
    }

    @Test
    void update_setsSelfParent_throws() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setParentId(1L);

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own parent");
    }

    @Test
    void delete_blockedWhenProductsExist() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(productRepository.countByCategoryIdAndDeletedAtIsNull(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("3 product");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_blockedWhenHasChildren() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sub-categories");
    }

    @Test
    void delete_successOnEmptyCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(productRepository.countByCategoryIdAndDeletedAtIsNull(1L)).thenReturn(0L);

        service.delete(1L);

        verify(translationRepository).deleteByCategoryId(1L);
        verify(categoryRepository).delete(category);
    }

    @Test
    void get_missing_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listTree_empty() {
        when(categoryRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of());
        assertThat(service.listTree()).isEmpty();
    }
}
