package com.uniform.store.service.impl;

import com.uniform.store.dto.response.BrandSummaryDto;
import com.uniform.store.entity.Brand;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.BrandTranslationRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ReviewRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock private BrandRepository brandRepository;
    @Mock private BrandTranslationRepository brandTranslationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks private BrandServiceImpl service;

    private Brand brand;

    @BeforeEach
    void setup() {
        brand = Brand.builder().slug("nike").name("Nike").logoUrl("https://logo").isActive(true).build();
        brand.setId(1L);
    }

    @Test
    void getBrandSummary_aggregatesStats_andRoundsRating() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandTranslationRepository.findByBrandIdInAndLocale(List.of(1L), "en")).thenReturn(List.of());
        when(productRepository.countByBrandIdAndIsActiveTrueAndDeletedAtIsNull(1L)).thenReturn(12L);
        when(orderItemRepository.sumSoldByBrandId(eq(1L), any())).thenReturn(345L);
        when(reviewRepository.aggregateRatingByBrandId(1L, ReviewStatus.APPROVED))
                .thenReturn(List.<Object[]>of(new Object[]{4.26, 8L}));

        BrandSummaryDto dto = service.getBrandSummary(1L, "en");

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Nike");
        assertThat(dto.getLogoUrl()).isEqualTo("https://logo");
        assertThat(dto.getProductCount()).isEqualTo(12L);
        assertThat(dto.getSoldCount()).isEqualTo(345L);
        assertThat(dto.getReviewCount()).isEqualTo(8L);
        assertThat(dto.getAverageRating()).isEqualTo(4.3);
    }

    @Test
    void getBrandSummary_noReviews_nullRating() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandTranslationRepository.findByBrandIdInAndLocale(List.of(1L), "en")).thenReturn(List.of());
        when(productRepository.countByBrandIdAndIsActiveTrueAndDeletedAtIsNull(1L)).thenReturn(3L);
        when(orderItemRepository.sumSoldByBrandId(eq(1L), any())).thenReturn(0L);
        when(reviewRepository.aggregateRatingByBrandId(1L, ReviewStatus.APPROVED))
                .thenReturn(List.<Object[]>of(new Object[]{null, 0L}));

        BrandSummaryDto dto = service.getBrandSummary(1L, "en");

        assertThat(dto.getReviewCount()).isZero();
        assertThat(dto.getAverageRating()).isNull();
        assertThat(dto.getSoldCount()).isZero();
    }

    @Test
    void getBrandSummary_missing_throwsNotFound() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getBrandSummary(99L, "en"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
