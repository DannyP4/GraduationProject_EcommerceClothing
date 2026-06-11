package com.uniform.store.service.impl;

import com.uniform.store.config.AppMailProperties;
import com.uniform.store.dto.mail.OrderEmailModel;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.Payment;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderEmailModelFactory {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm", Locale.ENGLISH).withZone(ZONE);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final AppMailProperties mailProps;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Transactional(readOnly = true)
    public OrderEmailModel build(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        var user = order.getUser();
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId).orElse(null);

        String currency = order.getCurrency();

        List<Map<String, Object>> itemRows = new ArrayList<>(items.size());
        for (OrderItem oi : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", oi.getProductName());
            row.put("variant", oi.getVariantLabel());
            row.put("sku", oi.getSku());
            row.put("quantity", oi.getQuantity());
            row.put("unitPrice", formatMoney(oi.getUnitPrice(), currency));
            row.put("lineTotal", formatMoney(oi.getLineTotal(), currency));
            itemRows.add(row);
        }

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("brandName", "Vesta");
        vars.put("recipientName", user.getFullName());
        vars.put("orderNumber", order.getOrderNumber());
        vars.put("statusLabel", statusLabel(order.getStatus()));
        vars.put("placedAt", DATE_FMT.format(order.getPlacedAt()));
        vars.put("items", itemRows);
        vars.put("subtotal", formatMoney(order.getSubtotal(), currency));
        vars.put("discountTotal", formatMoney(order.getDiscountTotal(), currency));
        vars.put("hasDiscount", order.getDiscountTotal() != null
                && order.getDiscountTotal().compareTo(BigDecimal.ZERO) > 0);
        vars.put("shippingCost", formatMoney(order.getShippingCost(), currency));
        vars.put("grandTotal", formatMoney(order.getGrandTotal(), currency));
        vars.put("paymentMethod", paymentLabel(payment));
        vars.put("shippingRecipient", order.getShippingRecipient());
        vars.put("shippingPhone", order.getShippingPhone());
        vars.put("shippingAddress", composeAddress(order));
        vars.put("orderUrl", frontendBaseUrl + "/account/orders/" + order.getOrderNumber());
        vars.put("supportEmail", mailProps.getSupportEmail());
        vars.put("year", Year.now().getValue());

        return new OrderEmailModel(user.getEmail(), vars);
    }

    private String composeAddress(Order order) {
        List<String> parts = new ArrayList<>();
        parts.add(order.getShippingLine1());
        if (order.getShippingWard() != null && !order.getShippingWard().isBlank()) {
            parts.add(order.getShippingWard());
        }
        parts.add(order.getShippingDistrict());
        parts.add(order.getShippingCity());
        parts.add(order.getShippingCountry());
        return String.join(", ", parts);
    }

    private String formatMoney(BigDecimal amount, String currency) {
        if (amount == null) return "—";
        if ("VND".equalsIgnoreCase(currency)) {
            NumberFormat nf = NumberFormat.getInstance(Locale.of("vi", "VN"));
            nf.setMaximumFractionDigits(0);
            return nf.format(amount) + "₫";
        }
        return amount.toPlainString() + " " + currency;
    }

    private String statusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case PAID -> "Paid";
            case PROCESSING -> "Processing";
            case SHIPPED -> "Shipped";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
            case REFUNDED -> "Refunded";
        };
    }

    private String paymentLabel(Payment payment) {
        if (payment == null) return "—";
        PaymentProvider provider = payment.getProvider();
        return switch (provider) {
            case COD -> "Cash on Delivery";
            case VNPAY -> "VNPAY";
            case STRIPE -> "Card (Stripe)";
            case BANK_TRANSFER -> "Bank Transfer";
        };
    }
}
