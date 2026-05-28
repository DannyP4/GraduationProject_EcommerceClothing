package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateProductImageRequest;
import com.uniform.store.dto.request.UpdateProductImageRequest;
import com.uniform.store.dto.response.AdminProductImageDto;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.service.AdminProductImageService;
import com.uniform.store.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductImageServiceImpl implements AdminProductImageService {

    private static final Logger log = LoggerFactory.getLogger(AdminProductImageServiceImpl.class);
    private static final int MAX_IMAGES_PER_PRODUCT = 8;

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ProductVariantRepository variantRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public List<AdminProductImageDto> listByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(productId).stream()
                .map(AdminProductImageServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AdminProductImageDto create(Long productId, CreateProductImageRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        long current = imageRepository.countByProductId(productId);
        if (current >= MAX_IMAGES_PER_PRODUCT) {
            throw new BadRequestException("Product already has " + MAX_IMAGES_PER_PRODUCT + " images (max). Delete one before adding another.");
        }

        ProductVariant variant = null;
        if (req.getVariantId() != null) {
            variant = variantRepository.findById(req.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant", req.getVariantId()));
            if (!Objects.equals(variant.getProduct().getId(), productId)) {
                throw new BadRequestException("Variant " + req.getVariantId() + " does not belong to product " + productId);
            }
        }

        boolean isPrimary = Boolean.TRUE.equals(req.getIsPrimary());
        if (isPrimary) {
            demoteCurrentPrimary(productId);
        } else if (current == 0) {
            isPrimary = true;
        }

        ProductImage saved = imageRepository.save(ProductImage.builder()
                .product(product)
                .variant(variant)
                .url(req.getUrl())
                .publicId(blankToNull(req.getPublicId()))
                .altText(blankToNull(req.getAltText()))
                .sortOrder(req.getSortOrder() == null ? (int) current : req.getSortOrder())
                .isPrimary(isPrimary)
                .build());
        return toDto(saved);
    }

    @Override
    @Transactional
    public AdminProductImageDto update(Long imageId, UpdateProductImageRequest req) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));

        if (req.getAltText() != null) image.setAltText(blankToNull(req.getAltText()));
        if (req.getSortOrder() != null) image.setSortOrder(req.getSortOrder());

        if (Boolean.TRUE.equals(req.getClearVariant())) {
            image.setVariant(null);
        } else if (req.getVariantId() != null) {
            ProductVariant variant = variantRepository.findById(req.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant", req.getVariantId()));
            if (!Objects.equals(variant.getProduct().getId(), image.getProduct().getId())) {
                throw new BadRequestException("Variant " + req.getVariantId() + " does not belong to product " + image.getProduct().getId());
            }
            image.setVariant(variant);
        }

        if (Boolean.TRUE.equals(req.getIsPrimary()) && !Boolean.TRUE.equals(image.getIsPrimary())) {
            demoteCurrentPrimary(image.getProduct().getId());
            image.setIsPrimary(true);
        } else if (Boolean.FALSE.equals(req.getIsPrimary()) && Boolean.TRUE.equals(image.getIsPrimary())) {
            image.setIsPrimary(false);
        }

        return toDto(image);
    }

    @Override
    @Transactional
    public void delete(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));
        String publicId = image.getPublicId();
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        Long productId = image.getProduct().getId();
        imageRepository.delete(image);

        if (publicId != null && !publicId.isBlank()) {
            try {
                cloudinaryService.deleteByPublicId(publicId);
            } catch (RuntimeException ex) {
                log.warn("Cloudinary delete failed for publicId={}: {}", publicId, ex.getMessage());
            }
        }

        if (wasPrimary) {
            imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(productId).stream()
                    .findFirst()
                    .ifPresent(next -> next.setIsPrimary(true));
        }
    }

    private void demoteCurrentPrimary(Long productId) {
        for (ProductImage other : imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(productId)) {
            if (Boolean.TRUE.equals(other.getIsPrimary())) {
                other.setIsPrimary(false);
            }
        }
    }

    private static AdminProductImageDto toDto(ProductImage img) {
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

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
