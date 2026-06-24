package com.uniform.store.config;

public final class CacheNames {

    public static final String CATEGORIES = "catalog:categories";
    public static final String BRANDS = "catalog:brands";
    public static final String BRAND_SUMMARIES = "catalog:brand-summaries";
    public static final String PRODUCT_LISTS = "products:lists";
    public static final String SIMILAR_PRODUCTS = "products:similar";
    public static final String FREQUENTLY_BOUGHT_TOGETHER = "products:fbt";
    public static final String SIMILAR_TO_SET = "products:similar-set";

    public static final String[] CATALOG_CACHES = {
            CATEGORIES,
            BRANDS,
            BRAND_SUMMARIES,
            PRODUCT_LISTS,
            SIMILAR_PRODUCTS,
            FREQUENTLY_BOUGHT_TOGETHER,
            SIMILAR_TO_SET
    };

    private CacheNames() {
    }
}
