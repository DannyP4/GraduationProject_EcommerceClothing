package com.uniform.store.service.impl;

import com.uniform.store.config.EvictCatalogCaches;
import com.uniform.store.dto.request.CreateVariantRequest;
import com.uniform.store.dto.request.UpdateVariantRequest;
import com.uniform.store.dto.response.AdminVariantDto;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.service.AdminVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVariantServiceImpl implements AdminVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public List<AdminVariantDto> listByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return variantRepository.findByProductIdOrderBySizeAscColorAsc(productId).stream()
                .map(AdminVariantServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    @EvictCatalogCaches
    public AdminVariantDto create(Long productId, CreateVariantRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (variantRepository.existsBySku(req.getSku())) {
            throw new BadRequestException("SKU already exists: " + req.getSku());
        }
        if (variantRepository.existsByProductIdAndSizeAndColor(productId, req.getSize(), req.getColor())) {
            throw new BadRequestException("Variant with size '" + req.getSize() + "' and color '" + req.getColor() + "' already exists for this product");
        }

        ProductVariant saved = variantRepository.save(ProductVariant.builder()
                .product(product)
                .sku(req.getSku())
                .size(req.getSize())
                .color(req.getColor())
                .colorHex(blankToNull(req.getColorHex()))
                .priceOverride(req.getPriceOverride())
                .stockQuantity(req.getStockQuantity() == null ? 0 : req.getStockQuantity())
                .weightGrams(req.getWeightGrams())
                .isActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive())
                .build());
        return toDto(saved);
    }

    @Override
    @Transactional
    @EvictCatalogCaches
    public AdminVariantDto update(Long variantId, UpdateVariantRequest req) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant", variantId));

        String newSize = req.getSize() != null && !req.getSize().isBlank() ? req.getSize() : variant.getSize();
        String newColor = req.getColor() != null && !req.getColor().isBlank() ? req.getColor() : variant.getColor();
        boolean comboChanged = !Objects.equals(newSize, variant.getSize()) || !Objects.equals(newColor, variant.getColor());
        if (comboChanged && variantRepository.existsByProductIdAndSizeAndColor(
                variant.getProduct().getId(), newSize, newColor)) {
            throw new BadRequestException("Variant with size '" + newSize + "' and color '" + newColor + "' already exists for this product");
        }

        variant.setSize(newSize);
        variant.setColor(newColor);
        if (req.getColorHex() != null) variant.setColorHex(blankToNull(req.getColorHex()));
        if (req.getPriceOverride() != null) variant.setPriceOverride(req.getPriceOverride());
        if (req.getStockQuantity() != null) variant.setStockQuantity(req.getStockQuantity());
        if (req.getWeightGrams() != null) variant.setWeightGrams(req.getWeightGrams());
        if (req.getIsActive() != null) variant.setIsActive(req.getIsActive());

        return toDto(variant);
    }

    @Override
    @Transactional
    @EvictCatalogCaches
    public void delete(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant", variantId));
        if (orderItemRepository.existsByVariantId(variantId)) {
            throw new BadRequestException("Cannot delete variant referenced by existing orders; deactivate it instead");
        }
        variantRepository.delete(variant);
    }

    private static AdminVariantDto toDto(ProductVariant v) {
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

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
