package com.uniform.store.service.impl;

import com.uniform.store.dto.request.ProductFilterRequest;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.ProductDetailDto;
import com.uniform.store.dto.response.ProductImageDto;
import com.uniform.store.dto.response.ProductSummaryDto;
import com.uniform.store.dto.response.ProductVariantDto;
import com.uniform.store.entity.CategoryTranslation;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductAttribute;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductTranslation;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.ProductSort;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.CategoryTranslationRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.ProductAttributeRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductTranslationRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.ReviewRepository;
import com.uniform.store.service.PricingService;
import com.uniform.store.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<OrderStatus> SOLD_STATUSES = List.of(
            OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductTranslationRepository productTranslationRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final PricingService pricingService;

    @Override
    public PageResponse<ProductSummaryDto> listProducts(ProductFilterRequest filter, Pageable pageable, String locale) {
        ProductSort sort = filter.getSort() != null ? filter.getSort() : ProductSort.NEWEST;
        int safeSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable effective = PageRequest.of(pageable.getPageNumber(), safeSize, sort.toSort());

        String searchTerm = (filter.getSearch() != null && !filter.getSearch().isBlank())
                ? filter.getSearch().trim()
                : null;

        Page<Product> page = productRepository.searchProducts(
                filter.getCategoryId(),
                filter.getBrandId(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                searchTerm,
                effective);

        List<Product> products = page.getContent();
        if (products.isEmpty()) {
            return PageResponse.from(page, List.of());
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        List<Long> categoryIds = products.stream().map(p -> p.getCategory().getId()).distinct().toList();

        Map<Long, ProductTranslation> productTranslations = productTranslationRepository
                .findByProductIdInAndLocale(productIds, locale)
                .stream()
                .collect(Collectors.toMap(t -> t.getProduct().getId(), Function.identity(), (a, b) -> a));

        Map<Long, CategoryTranslation> categoryTranslations = categoryTranslationRepository
                .findByCategoryIdInAndLocale(categoryIds, locale)
                .stream()
                .collect(Collectors.toMap(t -> t.getCategory().getId(), Function.identity(), (a, b) -> a));

        Map<Long, String> primaryImageUrls = new LinkedHashMap<>();
        for (ProductImage img : imageRepository.findThumbnailCandidatesByProductIds(productIds)) {
            primaryImageUrls.putIfAbsent(img.getProduct().getId(), img.getUrl());
        }

        Map<Long, double[]> ratingByProduct = new HashMap<>();
        for (Object[] row : reviewRepository.aggregateRatingByProductIds(productIds, ReviewStatus.APPROVED)) {
            double avg = row[1] == null ? 0 : ((Number) row[1]).doubleValue();
            long cnt = ((Number) row[2]).longValue();
            ratingByProduct.put((Long) row[0], new double[]{ avg, cnt });
        }

        Map<Long, Long> soldByProduct = new HashMap<>();
        for (Object[] row : orderItemRepository.aggregateSoldByProductIds(productIds, SOLD_STATUSES)) {
            soldByProduct.put((Long) row[0], ((Number) row[1]).longValue());
        }

        Instant now = Instant.now();
        List<ProductSummaryDto> mapped = products.stream()
                .map(p -> {
                    PricingService.EffectivePrice ep = pricingService.resolve(p, null, now);
                    return ProductSummaryDto.builder()
                        .id(p.getId())
                        .slug(p.getSlug())
                        .name(translatedProductName(p, productTranslations.get(p.getId())))
                        .basePrice(p.getBasePrice())
                        .salePrice(ep.onSale() ? ep.effectivePrice() : null)
                        .discountPercent(ep.discountPercent())
                        .currency(p.getCurrency())
                        .primaryImageUrl(primaryImageUrls.get(p.getId()))
                        .brandName(p.getBrand().getName())
                        .categoryName(translatedCategoryName(p, categoryTranslations.get(p.getCategory().getId())))
                        .averageRating(ratingByProduct.containsKey(p.getId())
                                ? Double.valueOf(Math.round(ratingByProduct.get(p.getId())[0] * 10.0) / 10.0)
                                : null)
                        .reviewCount(ratingByProduct.containsKey(p.getId())
                                ? (long) ratingByProduct.get(p.getId())[1]
                                : 0L)
                        .soldCount(soldByProduct.getOrDefault(p.getId(), 0L))
                        .build();
                })
                .toList();

        return PageResponse.from(page, mapped);
    }

    @Override
    public ProductDetailDto getProduct(String idOrSlug, String locale) {
        Optional<Product> productOpt;
        try {
            Long id = Long.parseLong(idOrSlug);
            productOpt = productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(id);
        } catch (NumberFormatException ignored) {
            productOpt = productRepository.findBySlugAndIsActiveTrueAndDeletedAtIsNull(idOrSlug);
        }

        Product product = productOpt.orElseThrow(
                () -> new ResourceNotFoundException("Product", "idOrSlug", idOrSlug));

        ProductTranslation pTranslation = productTranslationRepository
                .findByProductIdAndLocale(product.getId(), locale)
                .orElse(null);

        CategoryTranslation cTranslation = categoryTranslationRepository
                .findByCategoryIdInAndLocale(List.of(product.getCategory().getId()), locale)
                .stream().findFirst().orElse(null);

        List<ProductImage> images = imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(product.getId());
        List<ProductVariant> variants = variantRepository.findByProductIdAndIsActiveTrueOrderBySizeAscColorAsc(product.getId());
        List<ProductAttribute> attrs = attributeRepository.findByProductIdOrderByAttrKeyAsc(product.getId());

        Map<String, String> attributeMap = new LinkedHashMap<>();
        for (ProductAttribute a : attrs) {
            attributeMap.put(a.getAttrKey(), a.getAttrValue());
        }

        long reviewCount = reviewRepository.countByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED);
        Double averageRating = null;
        if (reviewCount > 0) {
            Double avg = reviewRepository.averageRating(product.getId(), ReviewStatus.APPROVED);
            averageRating = avg == null ? null : Math.round(avg * 10.0) / 10.0;
        }

        long soldCount = 0L;
        for (Object[] row : orderItemRepository.aggregateSoldByProductIds(List.of(product.getId()), SOLD_STATUSES)) {
            soldCount = ((Number) row[1]).longValue();
        }

        Instant now = Instant.now();
        PricingService.EffectivePrice headline = pricingService.resolve(product, null, now);

        return ProductDetailDto.builder()
                .id(product.getId())
                .slug(product.getSlug())
                .name(translatedProductName(product, pTranslation))
                .description(translatedProductDescription(product, pTranslation))
                .gender(product.getGender())
                .basePrice(product.getBasePrice())
                .salePrice(headline.onSale() ? headline.effectivePrice() : null)
                .discountPercent(headline.discountPercent())
                .saleEndsAt(headline.onSale() ? product.getSaleEndsAt() : null)
                .currency(product.getCurrency())
                .brandName(product.getBrand().getName())
                .brandSlug(product.getBrand().getSlug())
                .categoryName(translatedCategoryName(product, cTranslation))
                .categorySlug(product.getCategory().getSlug())
                .images(images.stream().map(this::toImageDto).toList())
                .variants(variants.stream()
                        .map(v -> toVariantDto(v, product, now))
                        .toList())
                .attributes(attributeMap)
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .soldCount(soldCount)
                .build();
    }

    private String translatedProductName(Product p, ProductTranslation t) {
        return (t != null && t.getName() != null && !t.getName().isBlank()) ? t.getName() : p.getName();
    }

    private String translatedProductDescription(Product p, ProductTranslation t) {
        return (t != null && t.getDescription() != null && !t.getDescription().isBlank())
                ? t.getDescription()
                : p.getDescription();
    }

    private String translatedCategoryName(Product p, CategoryTranslation t) {
        return (t != null && t.getName() != null && !t.getName().isBlank())
                ? t.getName()
                : p.getCategory().getName();
    }

    private ProductImageDto toImageDto(ProductImage img) {
        return ProductImageDto.builder()
                .id(img.getId())
                .url(img.getUrl())
                .altText(img.getAltText())
                .sortOrder(img.getSortOrder())
                .isPrimary(img.getIsPrimary())
                .variantId(img.getVariant() != null ? img.getVariant().getId() : null)
                .build();
    }

    private ProductVariantDto toVariantDto(ProductVariant v, Product product, Instant now) {
        PricingService.EffectivePrice ep = pricingService.resolve(product, v, now);
        return ProductVariantDto.builder()
                .id(v.getId())
                .sku(v.getSku())
                .size(v.getSize())
                .color(v.getColor())
                .colorHex(v.getColorHex())
                .price(ep.originalPrice())
                .salePrice(ep.onSale() ? ep.effectivePrice() : null)
                .discountPercent(ep.discountPercent())
                .stockQuantity(v.getStockQuantity())
                .isActive(v.getIsActive())
                .build();
    }
}
