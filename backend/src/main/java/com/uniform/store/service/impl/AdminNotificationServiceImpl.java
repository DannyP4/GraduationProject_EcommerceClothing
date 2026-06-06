package com.uniform.store.service.impl;

import com.uniform.store.config.CartProperties;
import com.uniform.store.dto.response.NotificationDto;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Review;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.ReviewRepository;
import com.uniform.store.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNotificationServiceImpl implements AdminNotificationService {

    static final int ORDER_LIMIT = 8;
    static final int STOCK_LIMIT = 8;
    static final int REVIEW_LIMIT = 5;

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ReviewRepository reviewRepository;
    private final CartProperties cartProperties;

    @Override
    public List<NotificationDto> feed() {
        List<NotificationDto> out = new ArrayList<>();

        orderRepository.findByStatusInOrderByPlacedAtDescIdDesc(
                        AdminStatsServiceImpl.OPEN_ORDER_STATUSES, PageRequest.of(0, ORDER_LIMIT))
                .forEach(o -> out.add(toOrder(o)));

        productVariantRepository.findLowStockWithProduct(cartProperties.getLowStockThreshold()).stream()
                .limit(STOCK_LIMIT)
                .forEach(v -> out.add(toStock(v)));

        reviewRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, REVIEW_LIMIT))
                .forEach(r -> out.add(toReview(r)));

        return out;
    }

    private NotificationDto toOrder(Order o) {
        return NotificationDto.builder()
                .id("order-" + o.getOrderNumber())
                .type("ORDER")
                .message("Order " + o.getOrderNumber() + " · " + o.getStatus() + " · " + vnd(o.getGrandTotal()))
                .href("/admin/orders/" + o.getOrderNumber())
                .at(o.getPlacedAt())
                .build();
    }

    private NotificationDto toStock(ProductVariant v) {
        int qty = v.getStockQuantity();
        String tail = qty == 0 ? "sold out" : qty + " left";
        return NotificationDto.builder()
                .id("stock-" + v.getId())
                .type("STOCK")
                .message(v.getProduct().getName() + " (" + v.getSku() + ") · " + tail)
                .href("/admin/products")
                .build();
    }

    private NotificationDto toReview(Review r) {
        return NotificationDto.builder()
                .id("review-" + r.getId())
                .type("REVIEW")
                .message(r.getRating() + "★ review · " + r.getProduct().getName())
                .href("/admin/reviews?review=" + r.getId())
                .at(r.getCreatedAt())
                .build();
    }

    private static String vnd(BigDecimal amount) {
        long whole = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        return String.format("%,d", whole).replace(',', '.') + "₫";
    }
}
