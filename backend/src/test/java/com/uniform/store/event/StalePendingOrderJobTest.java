package com.uniform.store.event;

import com.uniform.store.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StalePendingOrderJobTest {

    @Mock OrderService orderService;
    @InjectMocks StalePendingOrderJob job;

    @Test
    void cancelsEachCandidate() {
        when(orderService.findStalePendingOrderIds()).thenReturn(List.of(1L, 2L, 3L));
        when(orderService.autoCancelStaleOrder(anyLong())).thenReturn(true);

        job.cancelStalePendingOrders();

        verify(orderService).autoCancelStaleOrder(1L);
        verify(orderService).autoCancelStaleOrder(2L);
        verify(orderService).autoCancelStaleOrder(3L);
    }

    @Test
    void noCandidates_skipsCancellation() {
        when(orderService.findStalePendingOrderIds()).thenReturn(List.of());

        job.cancelStalePendingOrders();

        verify(orderService, never()).autoCancelStaleOrder(anyLong());
    }

    @Test
    void oneFailureDoesNotStopOthers() {
        when(orderService.findStalePendingOrderIds()).thenReturn(List.of(1L, 2L));
        when(orderService.autoCancelStaleOrder(1L)).thenThrow(new RuntimeException("boom"));
        when(orderService.autoCancelStaleOrder(2L)).thenReturn(true);

        job.cancelStalePendingOrders();

        verify(orderService).autoCancelStaleOrder(2L);
    }
}
