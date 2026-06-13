package com.uniform.store.service;

import com.uniform.store.dto.response.AutoTranslateReport;

public interface CatalogTranslationService {

    /**
     * Auto-translate the catalog into the target locale
     *
     * @param targetLocale  "vi" or "ja" (base catalog is English)
     * @param limitProducts optional cap on number of products processed this run (for trials); null = all
     * @param maxChars      optional source-character budget; stops early when exceeded; null = unlimited
     */
    AutoTranslateReport run(String targetLocale, Integer limitProducts, Long maxChars);
}
