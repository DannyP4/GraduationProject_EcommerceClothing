package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateBrandRequest;
import com.uniform.store.dto.request.UpdateBrandRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.BrandTranslation;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.BrandTranslationRepository;
import com.uniform.store.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBrandServiceImplTest {

    @Mock private BrandRepository brandRepository;
    @Mock private BrandTranslationRepository translationRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private AdminBrandServiceImpl service;

    private Brand brand;

    @BeforeEach
    void setup() {
        brand = Brand.builder().slug("uniform").name("Uniform").isActive(true).build();
        brand.setId(1L);
    }

    @Test
    void create_rejectsDuplicateSlug() {
        CreateBrandRequest req = new CreateBrandRequest();
        req.setSlug("uniform");
        req.setName("Uniform");
        when(brandRepository.existsBySlug("uniform")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("slug already exists");

        verify(brandRepository, never()).save(any());
    }

    @Test
    void create_persistsViAndEnTranslations() {
        CreateBrandRequest req = new CreateBrandRequest();
        req.setSlug("atlas");
        req.setName("Atlas");
        req.setDescriptionVi("Mô tả tiếng Việt");
        req.setDescriptionEn("English description");
        when(brandRepository.existsBySlug("atlas")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> {
            Brand b = inv.getArgument(0);
            b.setId(2L);
            return b;
        });
        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand));
        when(translationRepository.findByBrandIdAndLocale(2L, "vi")).thenReturn(Optional.empty());
        when(translationRepository.findByBrandIdAndLocale(2L, "en")).thenReturn(Optional.empty());
        when(productRepository.countByBrandIdAndDeletedAtIsNull(2L)).thenReturn(0L);

        service.create(req);

        verify(translationRepository, org.mockito.Mockito.times(2)).save(any(BrandTranslation.class));
    }

    @Test
    void update_clearsTranslationWhenDescriptionBlank() {
        BrandTranslation existing = BrandTranslation.builder()
                .brand(brand).locale("vi").description("Cũ").build();
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(translationRepository.findByBrandIdAndLocale(1L, "vi")).thenReturn(Optional.of(existing));
        when(translationRepository.findByBrandIdAndLocale(1L, "en")).thenReturn(Optional.empty());
        when(productRepository.countByBrandIdAndDeletedAtIsNull(1L)).thenReturn(0L);

        UpdateBrandRequest req = new UpdateBrandRequest();
        req.setDescriptionVi("");

        service.update(1L, req);

        verify(translationRepository).delete(existing);
    }

    @Test
    void delete_blockedWhenProductsExist() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(productRepository.countByBrandIdAndDeletedAtIsNull(1L)).thenReturn(5L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("5 product");

        verify(brandRepository, never()).delete(any());
    }

    @Test
    void delete_emptyBrand_succeeds() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(productRepository.countByBrandIdAndDeletedAtIsNull(1L)).thenReturn(0L);

        service.delete(1L);

        verify(translationRepository).deleteByBrandId(1L);
        verify(brandRepository).delete(brand);
    }

    @Test
    void get_missing_throws() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
