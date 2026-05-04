package com.uniform.store.enums;

import org.springframework.data.domain.Sort;

public enum ProductSort {
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "basePrice")),
    PRICE_DESC(Sort.by(Sort.Direction.DESC, "basePrice")),
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    POPULAR(Sort.by(Sort.Direction.DESC, "createdAt"));

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }

    public static ProductSort fromString(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        try {
            return ProductSort.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NEWEST;
        }
    }
}
