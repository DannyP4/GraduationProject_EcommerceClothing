package com.uniform.store.bootstrap;

import com.uniform.store.service.EmbeddingBackfillService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.embeddings.auto-reconcile.enabled", havingValue = "true")
@RequiredArgsConstructor
public class EmbeddingReconcileScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingReconcileScheduler.class);

    private final EmbeddingBackfillService backfillService;

    // Embeds products added or edited since the last pass; backfill is idempotent via content_hash.
    @Scheduled(
            initialDelayString = "${app.ai.embeddings.auto-reconcile.interval-ms:900000}",
            fixedDelayString = "${app.ai.embeddings.auto-reconcile.interval-ms:900000}") // 15 min
    public void reconcile() {
        EmbeddingBackfillService.BackfillReport report = backfillService.backfill(false, 0);
        if (report.embedded() > 0 || report.failed() > 0) {
            log.info("Embedding auto-reconcile: {}", report);
        }
    }
}
