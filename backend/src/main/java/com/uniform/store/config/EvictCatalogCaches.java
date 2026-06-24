package com.uniform.store.config;

import org.springframework.cache.annotation.CacheEvict;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@CacheEvict(cacheNames = {
        CacheNames.CATEGORIES,
        CacheNames.BRANDS,
        CacheNames.BRAND_SUMMARIES,
        CacheNames.PRODUCT_LISTS,
        CacheNames.SIMILAR_PRODUCTS,
        CacheNames.FREQUENTLY_BOUGHT_TOGETHER,
        CacheNames.SIMILAR_TO_SET
}, allEntries = true)
public @interface EvictCatalogCaches {
}
