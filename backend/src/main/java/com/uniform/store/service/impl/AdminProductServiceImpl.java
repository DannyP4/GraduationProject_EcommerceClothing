package com.uniform.store.service.impl;

import com.uniform.store.dto.request.AdminProductFilterRequest;
import com.uniform.store.dto.request.CreateProductRequest;
import com.uniform.store.dto.request.UpdateProductRequest;
import com.uniform.store.dto.response.AdminProductDetailDto;
import com.uniform.store.dto.response.AdminProductImageDto;
import com.uniform.store.dto.response.AdminProductSummaryDto;
import com.uniform.store.dto.response.AdminVariantDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductTranslation;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.enums.SaleType;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductTranslationRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.service.AdminProductService;
import com.uniform.store.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductServiceImpl implements AdminProductService {

    private static final Logger log = LoggerFactory.getLogger(AdminProductServiceImpl.class);

    private static final String LOCALE_VI = "vi";
    private static final String LOCALE_JA = "ja";

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductTranslationRepository translationRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public PageResponse<AdminProductSummaryDto> list(AdminProductFilterRequest filter, Pageable pageable) {
        String deletedMode = filter.getDeleted() == null ? "none" : filter.getDeleted().toLowerCase(Locale.ROOT);
        boolean includeDeleted = deletedMode.equals("both") || deletedMode.equals("only");
        boolean onlyDeleted = deletedMode.equals("only");
        if (!deletedMode.equals("none") && !deletedMode.equals("both") && !deletedMode.equals("only")) {
            throw new BadRequestException("Invalid deleted filter: expected one of none|only|both");
        }

        String search = (filter.getSearch() != null && !filter.getSearch().isBlank()) ? filter.getSearch().trim() : null;

        Page<Product> page = productRepository.searchAdmin(
                search,
                filter.getBrandId(),
                filter.getCategoryId(),
                filter.getGender(),
                filter.getIsActive(),
                includeDeleted,
                onlyDeleted,
                pageable);

        List<Long> productIds = page.getContent().stream().map(Product::getId).toList();
        Map<Long, String> primaryImageByProduct = new HashMap<>();
        Map<Long, Long> variantCountByProduct = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (ProductImage img : imageRepository.findThumbnailCandidatesByProductIds(productIds)) {
                primaryImageByProduct.putIfAbsent(img.getProduct().getId(), img.getUrl());
            }
            for (Long pid : productIds) {
                variantCountByProduct.put(pid, (long) variantRepository.findByProductIdOrderBySizeAscColorAsc(pid).size());
            }
        }

        List<AdminProductSummaryDto> mapped = page.getContent().stream()
                .map(p -> toSummary(p, primaryImageByProduct.get(p.getId()), variantCountByProduct.getOrDefault(p.getId(), 0L)))
                .toList();
        return PageResponse.from(page, mapped);
    }

    @Override
    public AdminProductDetailDto get(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        List<ProductVariant> variants = variantRepository.findByProductIdOrderBySizeAscColorAsc(id);
        List<ProductImage> images = imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(id);
        ProductTranslation vi = null;
        ProductTranslation ja = null;
        for (ProductTranslation t : translationRepository.findByProductId(id)) {
            if (LOCALE_VI.equals(t.getLocale())) vi = t;
            else if (LOCALE_JA.equals(t.getLocale())) ja = t;
        }
        return toDetail(product, variants, images, vi, ja);
    }

    @Override
    @Transactional
    public AdminProductDetailDto create(CreateProductRequest req) {
        if (productRepository.existsBySlug(req.getSlug())) {
            throw new BadRequestException("Product slug already exists: " + req.getSlug());
        }
        Brand brand = brandRepository.findById(req.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand", req.getBrandId()));
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));

        validateSale(req.getSaleType(), req.getSaleValue(), req.getSaleStartsAt(), req.getSaleEndsAt());

        Product saved = productRepository.save(Product.builder()
                .brand(brand)
                .category(category)
                .slug(req.getSlug())
                .name(req.getName())
                .description(blankToNull(req.getDescription()))
                .gender(req.getGender())
                .basePrice(req.getBasePrice())
                .saleType(req.getSaleType())
                .saleValue(req.getSaleValue())
                .saleStartsAt(req.getSaleStartsAt())
                .saleEndsAt(req.getSaleEndsAt())
                .currency("VND")
                .isActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive())
                .publishedAt(req.getPublishedAt())
                .build());

        upsertProductTranslation(saved, LOCALE_VI, req.getNameVi(), req.getDescriptionVi());
        upsertProductTranslation(saved, LOCALE_JA, req.getNameJa(), req.getDescriptionJa());

        return toDetail(saved, List.of(), List.of(),
                transientTranslation(LOCALE_VI, req.getNameVi(), req.getDescriptionVi()),
                transientTranslation(LOCALE_JA, req.getNameJa(), req.getDescriptionJa()));
    }

    @Override
    @Transactional
    public AdminProductDetailDto update(Long id, UpdateProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (req.getName() != null && !req.getName().isBlank()) product.setName(req.getName());
        if (req.getDescription() != null) product.setDescription(blankToNull(req.getDescription()));
        if (req.getBrandId() != null) {
            Brand brand = brandRepository.findById(req.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand", req.getBrandId()));
            product.setBrand(brand);
        }
        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));
            product.setCategory(category);
        }
        if (req.getGender() != null) product.setGender(req.getGender());
        if (req.getBasePrice() != null) product.setBasePrice(req.getBasePrice());
        if (Boolean.TRUE.equals(req.getClearSale())) {
            product.setSaleType(null);
            product.setSaleValue(null);
            product.setSaleStartsAt(null);
            product.setSaleEndsAt(null);
        } else if (req.getSaleType() != null) {
            validateSale(req.getSaleType(), req.getSaleValue(), req.getSaleStartsAt(), req.getSaleEndsAt());
            product.setSaleType(req.getSaleType());
            product.setSaleValue(req.getSaleValue());
            product.setSaleStartsAt(req.getSaleStartsAt());
            product.setSaleEndsAt(req.getSaleEndsAt());
        }
        if (req.getIsActive() != null) product.setIsActive(req.getIsActive());
        if (req.getPublishedAt() != null) product.setPublishedAt(req.getPublishedAt());

        if (req.getNameVi() != null || req.getDescriptionVi() != null) {
            upsertProductTranslation(product, LOCALE_VI, req.getNameVi(), req.getDescriptionVi());
        }
        if (req.getNameJa() != null || req.getDescriptionJa() != null) {
            upsertProductTranslation(product, LOCALE_JA, req.getNameJa(), req.getDescriptionJa());
        }

        return get(id);
    }

    private void validateSale(SaleType type, BigDecimal value, Instant startsAt, Instant endsAt) {
        if (type == null) {
            if (value != null || startsAt != null || endsAt != null) {
                throw new BadRequestException("saleType is required when any sale field is set");
            }
            return;
        }
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("saleValue must be greater than 0 when saleType is set");
        }
        if (type == SaleType.PERCENT && value.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("PERCENT sale value cannot exceed 100");
        }
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BadRequestException("sale_ends_at must be after sale_starts_at");
        }
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (product.getDeletedAt() != null) {
            throw new BadRequestException("Product already deleted");
        }
        product.setDeletedAt(Instant.now());
        product.setIsActive(false);
    }

    @Override
    @Transactional
    public AdminProductDetailDto restore(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (product.getDeletedAt() == null) {
            throw new BadRequestException("Product is not deleted");
        }
        product.setDeletedAt(null);
        return get(id);
    }

    @Override
    @Transactional
    public void hardDelete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (product.getDeletedAt() == null) {
            throw new BadRequestException("Permanent delete requires the product to be soft-deleted first");
        }

        List<ProductVariant> variants = variantRepository.findByProductIdOrderBySizeAscColorAsc(id);
        for (ProductVariant v : variants) {
            if (orderItemRepository.existsByVariantId(v.getId())) {
                throw new BadRequestException("Cannot permanently delete: variant " + v.getSku()
                        + " is referenced by historical orders. Restore the product instead.");
            }
        }

        List<ProductImage> images = imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(id);
        List<String> publicIdsToCleanup = new ArrayList<>();
        for (ProductImage img : images) {
            if (img.getPublicId() != null && !img.getPublicId().isBlank()) {
                publicIdsToCleanup.add(img.getPublicId());
            }
        }

        productRepository.delete(product);

        for (String publicId : publicIdsToCleanup) {
            try {
                cloudinaryService.deleteByPublicId(publicId);
            } catch (RuntimeException ex) {
                log.warn("Cloudinary delete failed during hard-delete for publicId={}: {}", publicId, ex.getMessage());
            }
        }
    }

    private static AdminProductSummaryDto toSummary(Product p, String primaryImageUrl, long variantCount) {
        return AdminProductSummaryDto.builder()
                .id(p.getId())
                .slug(p.getSlug())
                .name(p.getName())
                .gender(p.getGender())
                .basePrice(p.getBasePrice())
                .saleType(p.getSaleType())
                .saleValue(p.getSaleValue())
                .saleStartsAt(p.getSaleStartsAt())
                .saleEndsAt(p.getSaleEndsAt())
                .currency(p.getCurrency())
                .isActive(p.getIsActive())
                .deletedAt(p.getDeletedAt())
                .publishedAt(p.getPublishedAt())
                .brand(AdminProductSummaryDto.BrandRef.builder().id(p.getBrand().getId()).name(p.getBrand().getName()).build())
                .category(AdminProductSummaryDto.CategoryRef.builder().id(p.getCategory().getId()).name(p.getCategory().getName()).build())
                .primaryImageUrl(primaryImageUrl)
                .variantCount(variantCount)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private static AdminProductDetailDto toDetail(Product p, List<ProductVariant> variants, List<ProductImage> images,
                                                  ProductTranslation vi, ProductTranslation ja) {
        return AdminProductDetailDto.builder()
                .id(p.getId())
                .slug(p.getSlug())
                .name(p.getName())
                .description(p.getDescription())
                .nameVi(vi == null ? null : vi.getName())
                .nameJa(ja == null ? null : ja.getName())
                .descriptionVi(vi == null ? null : vi.getDescription())
                .descriptionJa(ja == null ? null : ja.getDescription())
                .gender(p.getGender())
                .basePrice(p.getBasePrice())
                .saleType(p.getSaleType())
                .saleValue(p.getSaleValue())
                .saleStartsAt(p.getSaleStartsAt())
                .saleEndsAt(p.getSaleEndsAt())
                .currency(p.getCurrency())
                .isActive(p.getIsActive())
                .publishedAt(p.getPublishedAt())
                .deletedAt(p.getDeletedAt())
                .brand(AdminProductSummaryDto.BrandRef.builder().id(p.getBrand().getId()).name(p.getBrand().getName()).build())
                .category(AdminProductSummaryDto.CategoryRef.builder().id(p.getCategory().getId()).name(p.getCategory().getName()).build())
                .variants(variants.stream().map(AdminProductServiceImpl::variantToDto).toList())
                .images(images.stream().map(AdminProductServiceImpl::imageToDto).toList())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private static AdminVariantDto variantToDto(ProductVariant v) {
        return AdminVariantDto.builder()
                .id(v.getId())
                .productId(v.getProduct().getId())
                .sku(v.getSku())
                .size(v.getSize())
                .color(v.getColor())
                .colorHex(v.getColorHex())
                .priceOverride(v.getPriceOverride())
                .stockQuantity(v.getStockQuantity())
                .weightGrams(v.getWeightGrams())
                .isActive(v.getIsActive())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private static AdminProductImageDto imageToDto(ProductImage img) {
        return AdminProductImageDto.builder()
                .id(img.getId())
                .productId(img.getProduct().getId())
                .variantId(img.getVariant() == null ? null : img.getVariant().getId())
                .url(img.getUrl())
                .publicId(img.getPublicId())
                .altText(img.getAltText())
                .sortOrder(img.getSortOrder())
                .isPrimary(img.getIsPrimary())
                .createdAt(img.getCreatedAt())
                .updatedAt(img.getUpdatedAt())
                .build();
    }

    private void upsertProductTranslation(Product product, String locale, String name, String description) {
        String n = blankToNull(name);
        String d = blankToNull(description);
        ProductTranslation existing = translationRepository.findByProductIdAndLocale(product.getId(), locale).orElse(null);
        if (n == null) {
            if (existing != null) translationRepository.delete(existing);
            return;
        }
        if (existing != null) {
            existing.setName(n);
            existing.setDescription(d);
            existing.setTranslatedAt(Instant.now());
        } else {
            translationRepository.save(ProductTranslation.builder()
                    .product(product).locale(locale).name(n).description(d)
                    .translatedAt(Instant.now()).build());
        }
    }

    private static ProductTranslation transientTranslation(String locale, String name, String description) {
        String n = blankToNull(name);
        if (n == null) return null;
        return ProductTranslation.builder()
                .locale(locale).name(n).description(blankToNull(description)).build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
