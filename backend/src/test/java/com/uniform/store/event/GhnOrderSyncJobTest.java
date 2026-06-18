package com.uniform.store.event;

import com.uniform.store.entity.Order;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.service.AdminOrderService;
import com.uniform.store.service.impl.GhnClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GhnOrderSyncJobTest {

    @Mock OrderRepository orderRepository;
    @Mock GhnClient ghnClient;
    @Mock AdminOrderService adminOrderService;
    @InjectMocks GhnOrderSyncJob job;

    @Test
    void deliveredStatus_marksOrderDelivered() {
        Order o = shipped(700L, "GHN700");
        when(ghnClient.isEnabled()).thenReturn(true);
        when(orderRepository.findByStatusAndGhnOrderCodeIsNotNull(OrderStatus.SHIPPED)).thenReturn(List.of(o));
        when(ghnClient.getOrderStatus("GHN700")).thenReturn(Optional.of("delivered"));
        when(adminOrderService.markDeliveredFromGhn(700L)).thenReturn(true);

        job.syncShippedOrders();

        verify(adminOrderService).markDeliveredFromGhn(700L);
    }

    @Test
    void inTransitStatus_doesNotMarkDelivered() {
        Order o = shipped(700L, "GHN700");
        when(ghnClient.isEnabled()).thenReturn(true);
        when(orderRepository.findByStatusAndGhnOrderCodeIsNotNull(OrderStatus.SHIPPED)).thenReturn(List.of(o));
        when(ghnClient.getOrderStatus("GHN700")).thenReturn(Optional.of("delivering"));

        job.syncShippedOrders();

        verify(adminOrderService, never()).markDeliveredFromGhn(anyLong());
    }

    @Test
    void ghnDisabled_skipsEntirely() {
        when(ghnClient.isEnabled()).thenReturn(false);

        job.syncShippedOrders();

        verify(orderRepository, never()).findByStatusAndGhnOrderCodeIsNotNull(any());
        verify(adminOrderService, never()).markDeliveredFromGhn(anyLong());
    }

    @Test
    void oneFailureDoesNotStopOthers() {
        Order failing = shipped(701L, "G1");
        Order ok = shipped(702L, "G2");
        when(ghnClient.isEnabled()).thenReturn(true);
        when(orderRepository.findByStatusAndGhnOrderCodeIsNotNull(OrderStatus.SHIPPED))
                .thenReturn(List.of(failing, ok));
        when(ghnClient.getOrderStatus("G1")).thenThrow(new RuntimeException("boom"));
        when(ghnClient.getOrderStatus("G2")).thenReturn(Optional.of("delivered"));
        when(adminOrderService.markDeliveredFromGhn(702L)).thenReturn(true);

        job.syncShippedOrders();

        verify(adminOrderService).markDeliveredFromGhn(702L);
        verify(adminOrderService, never()).markDeliveredFromGhn(701L);
    }

    private Order shipped(long id, String ghnCode) {
        Order o = Order.builder()
                .orderNumber("ORD-" + id)
                .status(OrderStatus.SHIPPED)
                .ghnOrderCode(ghnCode)
                .build();
        o.setId(id);
        return o;
    }
}
