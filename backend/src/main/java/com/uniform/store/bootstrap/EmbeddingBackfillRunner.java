package com.uniform.store.bootstrap;

import com.uniform.store.service.EmbeddingBackfillService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.embeddings.backfill-on-startup", havingValue = "true")
@RequiredArgsConstructor
public class EmbeddingBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingBackfillRunner.class);

    private final EmbeddingBackfillService backfillService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Startup embedding backfill enabled — running");
        EmbeddingBackfillService.BackfillReport report = backfillService.backfill(false, 0);
        log.info("Startup embedding backfill: {}", report);
    }
}
