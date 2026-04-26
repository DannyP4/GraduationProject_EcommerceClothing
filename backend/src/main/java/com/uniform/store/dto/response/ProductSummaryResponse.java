package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductSummaryResponse {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal basePrice;
    private BigDecimal comparePrice;
    private String badge;
    private String primaryImageUrl;
    private BigDecimal avgRating;
    private int reviewCount;
    private int totalSold;
    private boolean isVtoEnabled;
    private List<String> availableSizes;
    private List<String> availableColors;
    private String categorySlug;
    private String categoryName;
}
