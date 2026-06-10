package com.uniform.store.bootstrap;

import com.uniform.store.service.EmbeddingBackfillService;
import com.uniform.store.service.EmbeddingBackfillService.BackfillReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingReconcileSchedulerTest {

    @Mock
    private EmbeddingBackfillService backfillService;

    @InjectMocks
    private EmbeddingReconcileScheduler scheduler;

    @Test
    void reconcile_runsIdempotentBackfill() {
        when(backfillService.backfill(false, 0)).thenReturn(new BackfillReport(10, 2, 8, 0, 0));
        scheduler.reconcile();
        verify(backfillService).backfill(false, 0);
    }
}
