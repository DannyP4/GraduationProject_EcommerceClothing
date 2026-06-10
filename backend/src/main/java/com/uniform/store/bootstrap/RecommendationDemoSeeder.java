package com.uniform.store.bootstrap;

import com.uniform.store.entity.Category;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.OrderStatusHistory;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Seeds co-purchase orders that basket same-category in-stock products so FBT covers the whole catalog. Idempotent, dev-only.
@Component
@org.springframework.core.annotation.Order(30)
@RequiredArgsConstructor
public class RecommendationDemoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RecommendationDemoSeeder.class);
    private static final String ORDER_PREFIX = "FBT-";
    private static final int BASKET = 8;
    private static final int MAX_ORDERS = 1000;

    private final SeedProperties props;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!props.isEnabled()) {
            return;
        }
        if (orderRepository.existsByOrderNumber(ORDER_PREFIX + "00001")) {
            log.info("Recommendation demo seeder: co-purchase orders already present, skipping.");
            return;
        }

        User customer = userRepository.findByEmail("demo1@uniform.local")
                .orElseGet(() -> userRepository.findAll(PageRequest.of(0, 1)).stream().findFirst().orElse(null));
        if (customer == null) {
            log.info("Recommendation demo seeder: no user available, skipping.");
            return;
        }

        int n = 0;
        for (Category category : categoryRepository.findAll()) {
            if (n >= MAX_ORDERS) break;
            List<ProductVariant> picks = firstVariantPerProduct(variantRepository.findInStockByCategory(category.getId()));
            int i = 0;
            while (i + 1 < picks.size() && n < MAX_ORDERS) {
                int end = Math.min(i + BASKET, picks.size());
                if (picks.size() - end == 1) end = picks.size(); // absorb a lone trailing product
                n++;
                createOrder(String.format("%s%05d", ORDER_PREFIX, n), customer, picks.subList(i, end),
                        Instant.now().minus((n % 60) + 1, ChronoUnit.DAYS));
                i = end;
            }
        }

        log.info("Recommendation demo seeder: seeded {} co-purchase orders across categories.", n);
    }

    private List<ProductVariant> firstVariantPerProduct(List<ProductVariant> variants) {
        Map<Long, ProductVariant> byProduct = new LinkedHashMap<>();
        for (ProductVariant v : variants) {
            byProduct.putIfAbsent(v.getProduct().getId(), v);
        }
        return new ArrayList<>(byProduct.values());
    }

    private void createOrder(String orderNumber, User customer, List<ProductVariant> variants, Instant placedAt) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ProductVariant v : variants) {
            subtotal = subtotal.add(v.getProduct().getBasePrice());
        }
        BigDecimal shipping = BigDecimal.valueOf(30_000);
        BigDecimal grand = subtotal.add(shipping).setScale(4, RoundingMode.HALF_UP);

        Order order = orderRepository.save(Order.builder()
                .orderNumber(orderNumber)
                .user(customer)
                .status(OrderStatus.DELIVERED)
                .subtotal(subtotal.setScale(4, RoundingMode.HALF_UP))
                .discountTotal(BigDecimal.ZERO)
                .shippingCost(shipping.setScale(4, RoundingMode.HALF_UP))
                .taxTotal(BigDecimal.ZERO)
                .grandTotal(grand)
                .currency("VND")
                .shippingRecipient(customer.getFullName())
                .shippingPhone(customer.getPhone() != null ? customer.getPhone() : "0900000000")
                .shippingLine1("1 Demo Street")
                .shippingWard("Phuong 1")
                .shippingDistrict("Quan 1")
                .shippingCity("HCM")
                .shippingCountry("VN")
                .placedAt(placedAt)
                .build());

        for (ProductVariant v : variants) {
            BigDecimal unit = v.getProduct().getBasePrice();
            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .variant(v)
                    .productName(v.getProduct().getName())
                    .variantLabel(v.getSize() + " / " + v.getColor())
                    .sku(v.getSku())
                    .unitPrice(unit)
                    .quantity(1)
                    .lineTotal(unit.setScale(4, RoundingMode.HALF_UP))
                    .build());
        }

        orderHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.DELIVERED)
                .note("Seeded co-purchase order")
                .build());

        paymentRepository.save(Payment.builder()
                .order(order)
                .provider(PaymentProvider.COD)
                .providerTxnId("cod-fbt-" + order.getId())
                .amount(grand)
                .currency("VND")
                .status(PaymentStatus.CAPTURED)
                .paidAt(placedAt.plus(2, ChronoUnit.MINUTES))
                .build());
    }
}
