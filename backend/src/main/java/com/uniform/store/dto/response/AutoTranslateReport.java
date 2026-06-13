package com.uniform.store.dto.response;

public record AutoTranslateReport(
        String locale,
        int productsTranslated,
        int categoriesTranslated,
        int brandsTranslated,
        int skippedExisting,
        long charactersSent,
        boolean stoppedEarly,
        String note
) {}
