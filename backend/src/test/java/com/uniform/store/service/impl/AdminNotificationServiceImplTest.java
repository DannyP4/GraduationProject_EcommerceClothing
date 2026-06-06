package com.uniform.store.service.impl;

import com.uniform.store.config.CartProperties;
import com.uniform.store.dto.response.NotificationDto;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Review;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock ProductVariantRepository productVariantRepository;
    @Mock ReviewRepository reviewRepository;
    CartProperties cartProperties;

    AdminNotificationServiceImpl service;

    @BeforeEach
    void setup() {
        cartProperties = new CartProperties();
        service = new AdminNotificationServiceImpl(
                orderRepository, productVariantRepository, reviewRepository, cartProperties);
    }

    private Product product(String name) {
        return Product.builder().name(name).build();
    }

    @Test
    void feed_aggregatesOrderStockReviewInThatOrder() {
        Product p = product("Drop Shoulder Hoodie");

        Order order = Order.builder()
                .orderNumber("ORD-1").status(OrderStatus.PENDING)
                .grandTotal(new BigDecimal("500000")).placedAt(Instant.now())
                .build();

        ProductVariant variant = ProductVariant.builder()
                .product(p).sku("SKU-9").stockQuantity(0).build();
        variant.setId(9L);

        Review review = Review.builder().rating(2).product(p).build();
        review.setId(3L);
        review.setCreatedAt(Instant.now());

        when(orderRepository.findByStatusInOrderByPlacedAtDescIdDesc(any(), any()))
                .thenReturn(List.of(order));
        when(productVariantRepository.findLowStockWithProduct(anyInt()))
                .thenReturn(List.of(variant));
        when(reviewRepository.findAllByOrderByCreatedAtDescIdDesc(any()))
                .thenReturn(List.of(review));

        List<NotificationDto> feed = service.feed();

        assertThat(feed).extracting(NotificationDto::getType)
                .containsExactly("ORDER", "STOCK", "REVIEW");
        assertThat(feed).extracting(NotificationDto::getId)
                .containsExactly("order-ORD-1", "stock-9", "review-3");
        assertThat(feed.get(0).getMessage()).contains("PENDING").contains("500.000₫");
        assertThat(feed.get(0).getHref()).isEqualTo("/admin/orders/ORD-1");
        assertThat(feed.get(1).getMessage()).contains("SKU-9").contains("sold out");
        assertThat(feed.get(1).getAt()).isNull();
        assertThat(feed.get(2).getMessage()).contains("2★");
    }

    @Test
    void feed_queriesLowStockWithConfiguredThreshold() {
        cartProperties.setLowStockThreshold(3);
        when(orderRepository.findByStatusInOrderByPlacedAtDescIdDesc(any(), any())).thenReturn(List.of());
        when(productVariantRepository.findLowStockWithProduct(anyInt())).thenReturn(List.of());
        when(reviewRepository.findAllByOrderByCreatedAtDescIdDesc(any())).thenReturn(List.of());

        service.feed();

        verify(productVariantRepository).findLowStockWithProduct(eq(3));
    }

    @Test
    void feed_allEmpty_returnsEmptyList() {
        when(orderRepository.findByStatusInOrderByPlacedAtDescIdDesc(any(), any())).thenReturn(List.of());
        when(productVariantRepository.findLowStockWithProduct(anyInt())).thenReturn(List.of());
        when(reviewRepository.findAllByOrderByCreatedAtDescIdDesc(any())).thenReturn(List.of());

        assertThat(service.feed()).isEmpty();
    }
}
