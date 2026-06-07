package com.uniform.store.service;

public interface EmbeddingBackfillService {

    BackfillReport backfill(boolean force, int limit);

    record BackfillReport(int totalEmbeddable, int embedded, int skipped, int failed, int remaining) {
    }
}
