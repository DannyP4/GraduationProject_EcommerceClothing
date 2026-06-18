package com.uniform.store.event;

import com.uniform.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.orders.auto-cancel.enabled", havingValue = "true", matchIfMissing = true)
public class StalePendingOrderJob {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.orders.auto-cancel.interval-ms:300000}",
            initialDelayString = "${app.orders.auto-cancel.initial-delay-ms:60000}")
    public void cancelStalePendingOrders() {
        List<Long> ids = orderService.findStalePendingOrderIds();
        if (ids.isEmpty()) {
            return;
        }
        int cancelled = 0;
        for (Long id : ids) {
            try {
                if (orderService.autoCancelStaleOrder(id)) {
                    cancelled++;
                }
            } catch (Exception ex) {
                log.warn("Auto-cancel failed for order id {}: {}", id, ex.getMessage());
            }
        }
        log.info("Auto-cancel stale PENDING orders: {} cancelled of {} candidates", cancelled, ids.size());
    }
}
