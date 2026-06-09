package com.uniform.store.service.impl;

import com.uniform.store.dto.response.BrandDto;
import com.uniform.store.dto.response.BrandSummaryDto;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.BrandTranslation;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.BrandTranslationRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ReviewRepository;
import com.uniform.store.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private static final List<OrderStatus> SOLD_STATUSES = List.of(
            OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final BrandRepository brandRepository;
    private final BrandTranslationRepository brandTranslationRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public List<BrandDto> listBrands(String locale) {
        List<Brand> brands = brandRepository.findByIsActiveTrueOrderByNameAsc();
        if (brands.isEmpty()) {
            return List.of();
        }

        Map<Long, BrandTranslation> translations = brandTranslationRepository
                .findByBrandIdInAndLocale(
                        brands.stream().map(Brand::getId).toList(),
                        locale)
                .stream()
                .collect(Collectors.toMap(t -> t.getBrand().getId(), Function.identity(), (a, b) -> a));

        return brands.stream()
                .map(b -> BrandDto.builder()
                        .id(b.getId())
                        .slug(b.getSlug())
                        .name(b.getName())
                        .description(translations.containsKey(b.getId())
                                ? translations.get(b.getId()).getDescription()
                                : null)
                        .logoUrl(b.getLogoUrl())
                        .websiteUrl(b.getWebsiteUrl())
                        .build())
                .toList();
    }

    @Override
    public BrandSummaryDto getBrandSummary(Long id, String locale) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", String.valueOf(id)));

        String description = brandTranslationRepository.findByBrandIdInAndLocale(List.of(id), locale)
                .stream().findFirst().map(BrandTranslation::getDescription).orElse(null);

        long productCount = productRepository.countByBrandIdAndIsActiveTrueAndDeletedAtIsNull(id);
        long soldCount = orderItemRepository.sumSoldByBrandId(id, SOLD_STATUSES);

        Double averageRating = null;
        long reviewCount = 0L;
        List<Object[]> agg = reviewRepository.aggregateRatingByBrandId(id, ReviewStatus.APPROVED);
        if (!agg.isEmpty()) {
            Object[] row = agg.get(0);
            reviewCount = row[1] == null ? 0L : ((Number) row[1]).longValue();
            if (reviewCount > 0 && row[0] != null) {
                averageRating = Math.round(((Number) row[0]).doubleValue() * 10.0) / 10.0;
            }
        }

        return BrandSummaryDto.builder()
                .id(brand.getId())
                .slug(brand.getSlug())
                .name(brand.getName())
                .description(description)
                .logoUrl(brand.getLogoUrl())
                .websiteUrl(brand.getWebsiteUrl())
                .productCount(productCount)
                .soldCount(soldCount)
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .build();
    }
}
