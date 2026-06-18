package com.uniform.store.integration;

import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderAutoCancelIntegrationTest extends BaseIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired ProductVariantRepository variantRepository;

    User buyer;
    ProductVariant variant;

    @BeforeEach
    void seed() {
        buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        Brand brand = data.createBrand();
        Category category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        variant = data.createVariant(product, 7);
    }

    private Instant stale() {
        return Instant.now().minus(Duration.ofHours(1));
    }

    @Test
    void findStalePendingOrderIds_returnsOnlyStaleUnpaidOnlineOrders() {
        Order staleVnpay = data.createPendingOrder(buyer, variant, 1, PaymentProvider.VNPAY, PaymentStatus.PENDING, stale());
        data.createPendingOrder(buyer, variant, 1, PaymentProvider.VNPAY, PaymentStatus.PENDING, Instant.now());
        data.createPendingOrder(buyer, variant, 1, PaymentProvider.COD, PaymentStatus.PENDING, stale());
        data.createPendingOrder(buyer, variant, 1, PaymentProvider.VNPAY, PaymentStatus.CAPTURED, stale());

        List<Long> ids = orderService.findStalePendingOrderIds();

        assertThat(ids).containsExactly(staleVnpay.getId());
    }

    @Test
    void autoCancelStaleOrder_cancelsRestoresStockAndFailsPayment() {
        Order order = data.createPendingOrder(buyer, variant, 3, PaymentProvider.VNPAY, PaymentStatus.PENDING, stale());

        boolean cancelled = orderService.autoCancelStaleOrder(order.getId());

        assertThat(cancelled).isTrue();
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    @Test
    void autoCancelStaleOrder_isNoopWhenNotPending() {
        Order order = data.createPendingOrder(buyer, variant, 2, PaymentProvider.VNPAY, PaymentStatus.PENDING, stale());

        assertThat(orderService.autoCancelStaleOrder(order.getId())).isTrue();
        assertThat(orderService.autoCancelStaleOrder(order.getId())).isFalse();
        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(9);
    }
}
