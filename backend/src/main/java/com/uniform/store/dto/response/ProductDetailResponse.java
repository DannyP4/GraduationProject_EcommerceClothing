package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String materialSpecs;
    private BigDecimal basePrice;
    private BigDecimal comparePrice;
    private String badge;
    private BigDecimal avgRating;
    private int reviewCount;
    private int totalSold;
    private boolean isVtoEnabled;
    private String vtoModelUrl;
    private String categorySlug;
    private String categoryName;
    private List<VariantInfo> variants;
    private List<ImageInfo> images;

    @Data
    @Builder
    public static class VariantInfo {
        private Long id;
        private String size;
        private String color;
        private String sku;
        private BigDecimal priceDelta;
        private int stockQty;
        private boolean isActive;
    }

    @Data
    @Builder
    public static class ImageInfo {
        private Long id;
        private String url;
        private String altText;
        private boolean isPrimary;
        private short sortOrder;
    }
}
