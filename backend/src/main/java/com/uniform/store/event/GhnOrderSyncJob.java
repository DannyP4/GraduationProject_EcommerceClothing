package com.uniform.store.event;

import com.uniform.store.entity.Order;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.service.AdminOrderService;
import com.uniform.store.service.impl.GhnClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.shipping.ghn.status-sync.enabled", havingValue = "true", matchIfMissing = true)
public class GhnOrderSyncJob {

    private final OrderRepository orderRepository;
    private final GhnClient ghnClient;
    private final AdminOrderService adminOrderService;

    @Scheduled(fixedDelayString = "${app.shipping.ghn.status-sync.interval-ms:900000}",
            initialDelayString = "${app.shipping.ghn.status-sync.initial-delay-ms:120000}")
    public void syncShippedOrders() {
        if (!ghnClient.isEnabled()) {
            return;
        }
        List<Order> shipped = orderRepository.findByStatusAndGhnOrderCodeIsNotNull(OrderStatus.SHIPPED);
        if (shipped.isEmpty()) {
            return;
        }
        int delivered = 0;
        for (Order order : shipped) {
            try {
                boolean isDelivered = ghnClient.getOrderStatus(order.getGhnOrderCode())
                        .map(GhnClient::isDeliveredStatus).orElse(false);
                if (isDelivered && adminOrderService.markDeliveredFromGhn(order.getId())) {
                    delivered++;
                }
            } catch (Exception ex) {
                log.warn("GHN status sync failed for order {}: {}", order.getOrderNumber(), ex.getMessage());
            }
        }
        log.info("GHN status sync: {} of {} shipped orders marked delivered", delivered, shipped.size());
    }
}
