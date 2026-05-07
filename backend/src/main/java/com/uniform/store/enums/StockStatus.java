package com.uniform.store.enums;

public enum StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
    // Variant or its parent product was deactivated/soft-deleted after user added it.
    UNAVAILABLE
}
