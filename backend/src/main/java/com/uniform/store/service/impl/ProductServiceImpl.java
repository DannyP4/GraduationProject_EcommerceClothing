package com.uniform.store.service.impl;

import com.uniform.store.dto.response.ProductDetailResponse;
import com.uniform.store.dto.response.ProductSummaryResponse;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Page<ProductSummaryResponse> getProducts(String category, String keyword, String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.join("category").get("slug"), category));
            }

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable)
                .map(this::toSummary);
    }

    @Override
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));

        return toDetail(product);
    }

    @Override
    public List<ProductSummaryResponse> getRelatedProducts(String slug) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));

        return product.getRelatedProducts().stream()
                .filter(Product::isActive)
                .map(this::toSummary)
                .toList();
    }

    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by("createdAt").descending();
        return switch (sort) {
            case "price_asc"  -> Sort.by("basePrice").ascending();
            case "price_desc" -> Sort.by("basePrice").descending();
            case "popular"    -> Sort.by("totalSold").descending();
            case "rating"     -> Sort.by("avgRating").descending();
            default           -> Sort.by("createdAt").descending();
        };
    }

    private ProductSummaryResponse toSummary(Product p) {
        String primaryImageUrl = p.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getUrl)
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl());

        List<String> sizes = p.getVariants().stream()
                .filter(ProductVariant::isActive)
                .map(v -> v.getSize().name())
                .distinct().toList();

        List<String> colors = p.getVariants().stream()
                .filter(ProductVariant::isActive)
                .map(ProductVariant::getColor)
                .distinct().toList();

        return ProductSummaryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .basePrice(p.getBasePrice())
                .comparePrice(p.getComparePrice())
                .badge(p.getBadge() != null ? p.getBadge().name() : null)
                .primaryImageUrl(primaryImageUrl)
                .avgRating(p.getAvgRating())
                .reviewCount(p.getReviewCount())
                .totalSold(p.getTotalSold())
                .isVtoEnabled(p.isVtoEnabled())
                .availableSizes(sizes)
                .availableColors(colors)
                .categorySlug(p.getCategory().getSlug())
                .categoryName(p.getCategory().getName())
                .build();
    }

    private ProductDetailResponse toDetail(Product p) {
        List<ProductDetailResponse.VariantInfo> variants = p.getVariants().stream()
                .map(v -> ProductDetailResponse.VariantInfo.builder()
                        .id(v.getId())
                        .size(v.getSize().name())
                        .color(v.getColor())
                        .sku(v.getSku())
                        .priceDelta(v.getPriceDelta())
                        .stockQty(v.getStockQty())
                        .isActive(v.isActive())
                        .build())
                .toList();

        List<ProductDetailResponse.ImageInfo> images = p.getImages().stream()
                .map(i -> ProductDetailResponse.ImageInfo.builder()
                        .id(i.getId())
                        .url(i.getUrl())
                        .altText(i.getAltText())
                        .isPrimary(i.isPrimary())
                        .sortOrder(i.getSortOrder())
                        .build())
                .toList();

        return ProductDetailResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .materialSpecs(p.getMaterialSpecs())
                .basePrice(p.getBasePrice())
                .comparePrice(p.getComparePrice())
                .badge(p.getBadge() != null ? p.getBadge().name() : null)
                .avgRating(p.getAvgRating())
                .reviewCount(p.getReviewCount())
                .totalSold(p.getTotalSold())
                .isVtoEnabled(p.isVtoEnabled())
                .vtoModelUrl(p.getVtoModelUrl())
                .categorySlug(p.getCategory().getSlug())
                .categoryName(p.getCategory().getName())
                .variants(variants)
                .images(images)
                .build();
    }
}
