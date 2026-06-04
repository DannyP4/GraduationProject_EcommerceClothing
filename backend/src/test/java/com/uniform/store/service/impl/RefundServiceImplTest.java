package com.uniform.store.service.impl;

import com.uniform.store.dto.response.AdminOrderDetailDto;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.OrderStatusHistory;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.mapper.OrderMapper;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.StripeService;
import com.uniform.store.service.VnpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock OrderStatusHistoryRepository statusHistoryRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock UserRepository userRepository;
    @Mock OrderMapper orderMapper;
    @Mock StripeService stripeService;
    @Mock VnpayService vnpayService;

    RefundServiceImpl refundService;

    User adminUser;
    User customer;
    Order order;
    ProductVariant variant;
    OrderItem orderItem;
    Payment payment;

    @BeforeEach
    void setUp() {
        refundService = new RefundServiceImpl(
                orderRepository, orderItemRepository, statusHistoryRepository, paymentRepository,
                variantRepository, userRepository, orderMapper, stripeService, vnpayService);

        adminUser = User.builder()
                .email("admin@uniform.test").fullName("Boss")
                .role(Role.builder().name("admin").displayName("Administrator").build())
                .status(UserStatus.ACTIVE).build();
        adminUser.setId(1L);

        customer = User.builder()
                .email("buyer@uniform.test").fullName("Buyer")
                .role(Role.builder().name("customer").displayName("Customer").build())
                .status(UserStatus.ACTIVE).build();
        customer.setId(2L);

        Product product = Product.builder()
                .slug("p1").name("Tee").basePrice(new BigDecimal("250000")).currency("VND").build();
        product.setId(100L);

        variant = ProductVariant.builder()
                .product(product).sku("SKU-1").size("M").color("Black")
                .stockQuantity(18).isActive(true).build();
        variant.setId(200L);

        order = Order.builder()
                .orderNumber("UNF-20260530-0001").user(customer)
                .status(OrderStatus.PAID)
                .subtotal(new BigDecimal("500000")).grandTotal(new BigDecimal("530000"))
                .currency("VND")
                .shippingRecipient("Buyer").shippingPhone("0912345678")
                .shippingLine1("123 Main").shippingDistrict("Q1").shippingCity("HCM").shippingCountry("VN")
                .placedAt(Instant.now()).build();
        order.setId(700L);

        orderItem = OrderItem.builder()
                .order(order).variant(variant)
                .productName("Tee").variantLabel("M / Black").sku("SKU-1")
                .unitPrice(new BigDecimal("250000")).quantity(2).lineTotal(new BigDecimal("500000"))
                .build();
        orderItem.setId(800L);

        payment = Payment.builder()
                .order(order).provider(PaymentProvider.COD).providerTxnId("cod-700")
                .amount(new BigDecimal("530000")).currency("VND")
                .status(PaymentStatus.CAPTURED).paidAt(Instant.now()).build();
        payment.setId(900L);
    }

    @Test
    void refund_paidCodOrder_setsRefundedRestoresStockNoGateway() {
        stubLookups();
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));

        AdminOrderDetailDto dto = refundService.refundOrder("UNF-20260530-0001", "duplicate", "admin@uniform.test");

        assertThat(dto).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(variant.getStockQuantity()).as("18 + 2 restored").isEqualTo(20);
        verify(stripeService, never()).refund(any(), org.mockito.ArgumentMatchers.anyLong());
        verify(vnpayService, never()).refund(any(), any(), any());

        ArgumentCaptor<OrderStatusHistory> cap = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(statusHistoryRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(cap.getValue().getNote()).contains("admin@uniform.test").contains("COD").contains("duplicate");
    }

    @Test
    void refund_processingVnpayOrder_callsVnpaySimulateAndRestoresStock() {
        order.setStatus(OrderStatus.PROCESSING);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setProviderTxnId("vnp-700");
        stubLookups();
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));
        when(vnpayService.refund(eq("UNF-20260530-0001"), any(), eq("vnp-700")))
                .thenReturn(new VnpayService.RefundResult("VNPAY-REFUND-x", "SIMULATED", Map.of()));

        refundService.refundOrder("UNF-20260530-0001", null, "admin@uniform.test");

        verify(vnpayService).refund(eq("UNF-20260530-0001"), any(), eq("vnp-700"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(variant.getStockQuantity()).isEqualTo(20);
    }

    @Test
    void refund_shippedStripeOrder_callsStripeWithOriginalUsdAndDoesNotRestoreStock() {
        order.setStatus(OrderStatus.SHIPPED);
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setProviderTxnId("cs_test_abc");
        payment.setAmount(new BigDecimal("9.63"));
        payment.setCurrency("USD");
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("paymentIntent", "pi_test_xyz");
        payment.setRawResponse(resp);
        stubLookups();
        when(stripeService.refund("pi_test_xyz", 963L))
                .thenReturn(new StripeService.RefundResult("re_test_1", "succeeded"));

        refundService.refundOrder("UNF-20260530-0001", null, "admin@uniform.test");

        verify(stripeService).refund("pi_test_xyz", 963L);
        verify(variantRepository, never()).findAllByIdInWithProductForUpdate(anyCollection());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refund_deliveredOrder_doesNotRestoreStock() {
        order.setStatus(OrderStatus.DELIVERED);
        stubLookups();

        refundService.refundOrder("UNF-20260530-0001", null, "admin@uniform.test");

        verify(variantRepository, never()).findAllByIdInWithProductForUpdate(anyCollection());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    void refund_cancelledOrderWithCapturedPayment_refundsPaymentButKeepsOrderCancelled() {
        order.setStatus(OrderStatus.CANCELLED);
        stubLookups();

        refundService.refundOrder("UNF-20260530-0001", null, "admin@uniform.test");

        assertThat(order.getStatus()).as("order stays CANCELLED").isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(orderRepository, never()).save(any());
        verify(variantRepository, never()).findAllByIdInWithProductForUpdate(anyCollection());

        ArgumentCaptor<OrderStatusHistory> cap = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(statusHistoryRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void refund_pendingOrder_throwsBadRequest() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findByOrderNumber("UNF-20260530-0001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> refundService.refundOrder("UNF-20260530-0001", null, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot refund order in status PENDING");

        verify(paymentRepository, never()).findFirstByOrderIdOrderByIdDesc(any());
    }

    @Test
    void refund_alreadyRefunded_throwsBadRequest() {
        order.setStatus(OrderStatus.REFUNDED);
        when(orderRepository.findByOrderNumber("UNF-20260530-0001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> refundService.refundOrder("UNF-20260530-0001", null, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot refund order in status REFUNDED");
    }

    @Test
    void refund_paymentNotCaptured_throwsBadRequest() {
        payment.setStatus(PaymentStatus.PENDING);
        when(orderRepository.findByOrderNumber("UNF-20260530-0001")).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(700L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> refundService.refundOrder("UNF-20260530-0001", null, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only captured payments");
    }

    @Test
    void refund_stripeWithoutPaymentIntent_throwsBadRequest() {
        order.setStatus(OrderStatus.SHIPPED);
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setRawResponse(null);
        when(orderRepository.findByOrderNumber("UNF-20260530-0001")).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(700L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> refundService.refundOrder("UNF-20260530-0001", null, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("payment intent not found");
    }

    @Test
    void refund_orderNotFound_throws404() {
        when(orderRepository.findByOrderNumber("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundService.refundOrder("MISSING", null, "admin@uniform.test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubLookups() {
        when(orderRepository.findByOrderNumber("UNF-20260530-0001")).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(700L)).thenReturn(Optional.of(payment));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(700L)).thenReturn(List.of(orderItem));
        when(userRepository.findByEmail("admin@uniform.test")).thenReturn(Optional.of(adminUser));
        when(orderMapper.toAdminDetailDto(any(), any()))
                .thenReturn(AdminOrderDetailDto.builder().build());
    }
}
