package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDetailDto {

    private Long id;
    private String orderNumber;
    private OrderStatus status;

    private List<OrderItemDto> items;

    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal shippingCost;
    private BigDecimal taxTotal;
    private BigDecimal grandTotal;
    private String currency;

    // Shipping snapshot — surfaces directly to FE so the invoice display is faithful to placement time.
    private String shippingRecipient;
    private String shippingPhone;
    private String shippingLine1;
    private String shippingWard;
    private String shippingDistrict;
    private String shippingCity;
    private String shippingCountry;
    private String shippingPostalCode;

    private String notes;
    private Instant placedAt;

    private List<OrderStatusHistoryDto> statusHistory;
    private PaymentDto payment;

    // True only when order is in PENDING — drives the FE Cancel button visibility.
    private Boolean cancellable;
}
