package com.uniform.store.config;

import com.uniform.store.dto.request.ProductFilterRequest;
import com.uniform.store.enums.ProductSort;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class CacheKeyUtils {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RECOMMENDATION_SIZE = 20;

    private CacheKeyUtils() {
    }

    public static String productList(ProductFilterRequest filter, Pageable pageable, String locale) {
        ProductFilterRequest safeFilter = filter != null ? filter : new ProductFilterRequest();
        ProductSort sort = safeFilter.getSort() != null ? safeFilter.getSort() : ProductSort.NEWEST;
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return String.join(":",
                normalize(locale),
                "page=" + pageable.getPageNumber(),
                "size=" + size,
                "sort=" + sort.name(),
                "category=" + value(safeFilter.getCategoryId()),
                "brand=" + value(safeFilter.getBrandId()),
                "min=" + decimal(safeFilter.getMinPrice()),
                "max=" + decimal(safeFilter.getMaxPrice()),
                "search=" + normalize(safeFilter.getSearch()));
    }

    public static String recommendation(Long productId, int limit, String locale) {
        return normalize(locale) + ":product=" + productId + ":limit=" + clampRecommendationLimit(limit);
    }

    public static String similarSet(List<Long> ids, int limit, String locale) {
        String seedKey = ids == null ? "" : ids.stream()
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return normalize(locale) + ":ids=" + seedKey + ":limit=" + clampRecommendationLimit(limit);
    }

    private static int clampRecommendationLimit(int limit) {
        return limit <= 0 ? 0 : Math.min(limit, MAX_RECOMMENDATION_SIZE);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
