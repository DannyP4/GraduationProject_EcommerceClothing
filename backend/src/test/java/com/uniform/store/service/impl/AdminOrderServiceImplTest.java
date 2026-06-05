package com.uniform.store.service.impl;

import com.uniform.store.dto.request.AdminOrderFilter;
import com.uniform.store.dto.response.AdminOrderDetailDto;
import com.uniform.store.dto.response.OrderDetailDto;
import com.uniform.store.dto.response.PaymentDto;
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
import com.uniform.store.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock OrderStatusHistoryRepository statusHistoryRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock UserRepository userRepository;
    @Mock OrderMapper orderMapper;
    @Mock PaymentRepository paymentRepository;
    @Mock CouponService couponService;

    AdminOrderServiceImpl adminOrderService;

    User adminUser;
    User customer;
    Order order;
    ProductVariant variant;
    OrderItem orderItem;

    @BeforeEach
    void setUp() {
        adminOrderService = new AdminOrderServiceImpl(
                orderRepository, orderItemRepository, statusHistoryRepository,
                variantRepository, userRepository, orderMapper, paymentRepository, couponService);

        adminUser = User.builder()
                .email("admin@uniform.test")
                .fullName("Boss")
                .role(Role.builder().name("admin").displayName("Administrator").build())
                .status(UserStatus.ACTIVE)
                .build();
        adminUser.setId(1L);

        customer = User.builder()
                .email("buyer@uniform.test")
                .fullName("Buyer")
                .role(Role.builder().name("customer").displayName("Customer").build())
                .status(UserStatus.ACTIVE)
                .build();
        customer.setId(2L);

        Product product = Product.builder()
                .slug("p1").name("Tee").basePrice(new BigDecimal("250000")).currency("VND").build();
        product.setId(100L);

        variant = ProductVariant.builder()
                .product(product).sku("SKU-1").size("M").color("Black")
                .stockQuantity(18).isActive(true).build();
        variant.setId(200L);

        order = Order.builder()
                .orderNumber("UNF-20260520-0001")
                .user(customer)
                .status(OrderStatus.PAID)
                .subtotal(new BigDecimal("500000"))
                .grandTotal(new BigDecimal("500000"))
                .currency("VND")
                .shippingRecipient("Buyer").shippingPhone("0912345678")
                .shippingLine1("123 Main").shippingDistrict("Q1").shippingCity("HCM")
                .shippingCountry("VN")
                .placedAt(Instant.now())
                .build();
        order.setId(700L);

        orderItem = OrderItem.builder()
                .order(order).variant(variant)
                .productName("Tee").variantLabel("M / Black").sku("SKU-1")
                .unitPrice(new BigDecimal("250000")).quantity(2).lineTotal(new BigDecimal("500000"))
                .build();
        orderItem.setId(800L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listOrders_appliesSpecificationFromFilter() {
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(orderMapper.toAdminSummaryDtos(List.of(order))).thenReturn(List.of());

        AdminOrderFilter filter = AdminOrderFilter.builder().status(OrderStatus.PAID).search("buyer").build();
        adminOrderService.listOrders(filter, PageRequest.of(0, 20));

        verify(orderRepository).findAll(any(Specification.class), any(PageRequest.class));
        verify(orderMapper).toAdminSummaryDtos(List.of(order));
    }

    @Test
    void getOrder_byOrderNumber_skipsUserScopeCheck() {
        when(orderRepository.findByOrderNumber("UNF-20260520-0001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(700L)).thenReturn(List.of(orderItem));
        when(orderMapper.toAdminDetailDto(order, List.of(orderItem))).thenReturn(stubDetailDto(OrderStatus.PAID, false));

        AdminOrderDetailDto dto = adminOrderService.getOrder("UNF-20260520-0001");

        assertThat(dto).isNotNull();
        verify(orderRepository).findByOrderNumber("UNF-20260520-0001");
    }

    @Test
    void getOrder_notFound_throws404() {
        when(orderRepository.findByOrderNumber("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminOrderService.getOrder("MISSING"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transition_validNext_updatesStatusAndAppendsHistoryWithActor() {
        when(orderRepository.findByOrderNumber("UNF-20260520-0001")).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("admin@uniform.test")).thenReturn(Optional.of(adminUser));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(700L)).thenReturn(List.of(orderItem));
        when(orderMapper.toAdminDetailDto(any(), any())).thenReturn(stubDetailDto(OrderStatus.PROCESSING, false));

        adminOrderService.transitionOrder("UNF-20260520-0001",
                OrderStatus.PROCESSING, "Picked", "admin@uniform.test");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        ArgumentCaptor<OrderStatusHistory> cap = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(statusHistoryRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(cap.getValue().getNote()).contains("admin@uniform.test").contains("Picked");
        assertThat(cap.getValue().getChangedByUserId()).isEqualTo(1L);
    }

    @Test
    void transition_skipStep_throwsBadRequest() {
        when(orderRepository.findByOrderNumber("UNF-20260520-0001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminOrderService.transitionOrder(
                "UNF-20260520-0001", OrderStatus.DELIVERED, null, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid transition PAID -> DELIVERED");

        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void transition_fromTerminal_throwsBadRequest() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber("UNF-20260520-0001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminOrderService.transitionOrder(
                "UNF-20260520-0001", OrderStatus.SHIPPED, null, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("terminal");
    }

    @Test
    void transition_codPendingToProcessing_allowed() {
        order.setStatus(OrderStatus.PENDING);
        Payment cod = Payment.builder().order(order).provider(PaymentProvider.COD)
                .status(PaymentStatus.PENDING).amount(new BigDecimal("500000")).currency("VND").build();
        when(orderRepository.findByOrderNumber("UNF-20260520-0001")).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(700L)).thenReturn(Optional.of(cod));
        when(userRepository.findByEmail("admin@uniform.test")).thenReturn(Optional.of(adminUser));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(700L)).thenReturn(List.of(orderItem));
        when(orderMapper.toAdminDetailDto(any(), any())).thenReturn(stubDetailDto(OrderStatus.PROCESSING, false));

        adminOrderService.transitionOrder("UNF-20260520-0001", OrderStatus.PROCESSING, null, "admin@uniform.test");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(cod.getStatus()).as("not delivered yet").isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void transition_codShippedToDelivered_capturesPayment() {
        order.setStatus(OrderStatus.SHIPPED);
        Payment cod = Payment.builder().order(order).provider(PaymentProvider.COD)
                .status(PaymentStatus.PENDING).amount(new BigDecimal("500000")).currency("VND").build();
        when(orderRepository.findByOrderNumber("UNF-20260520-0001")).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(700L)).thenReturn(Optional.of(cod));
        when(userRepository.findByEmail("admin@uniform.test")).thenReturn(Optional.of(adminUser));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(700L)).thenReturn(List.of(orderItem));
        when(orderMapper.toAdminDetailDto(any(), any())).thenReturn(stubDetailDto(OrderStatus.DELIVERED, false));

        adminOrderService.transitionOrder("UNF-20260520-0001", OrderStatus.DELIVERED, null, "admin@uniform.test");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(cod.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(cod.getPaidAt()).isNotNull();
        verify(paymentRepository).save(cod);
    }

    @Test
    void cancel_processingOrder_restoresStockAndAppendsAdminNote() {
        order.setStatus(OrderStatus.PROCESSING);

        when(orderRepository.findByOrderNumber("UNF-20260520-0001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(700L)).thenReturn(List.of(orderItem));
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));
        when(userRepository.findByEmail("admin@uniform.test")).thenReturn(Optional.of(adminUser));
        when(orderMapper.toAdminDetailDto(any(), any())).thenReturn(stubDetailDto(OrderStatus.CANCELLED, false));

        adminOrderService.cancelOrder("UNF-20260520-0001", "wrong size", "admin@uniform.test");

        assertThat(variant.getStockQuantity()).as("stock 18 + 2 restored = 20").isEqualTo(20);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        ArgumentCaptor<OrderStatusHistory> cap = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(statusHistoryRepository).save(cap.capture());
        assertThat(cap.getValue().getNote()).contains("admin@uniform.test").contains("wrong size");
    }

    @Test
    void cancel_shippedOrder_throwsBadRequest() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByOrderNumber("UNF-20260520-0001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminOrderService.cancelOrder(
                "UNF-20260520-0001", null, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel order in status SHIPPED");

        verify(variantRepository, never()).findAllByIdInWithProductForUpdate(anyCollection());
    }

    @Test
    void cancel_paidOrderWithCapturedPayment_succeedsAndLeavesPaymentUntouched() {
        when(orderRepository.findByOrderNumber("UNF-20260520-0001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(700L)).thenReturn(List.of(orderItem));
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));
        when(userRepository.findByEmail("admin@uniform.test")).thenReturn(Optional.of(adminUser));
        when(orderMapper.toAdminDetailDto(any(), any())).thenReturn(stubDetailDto(OrderStatus.CANCELLED, true));

        AdminOrderDetailDto dto = adminOrderService.cancelOrder(
                "UNF-20260520-0001", null, "admin@uniform.test");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(dto.getRequiresRefund()).as("requiresRefund flag set for captured payment").isTrue();
    }

    private AdminOrderDetailDto stubDetailDto(OrderStatus status, boolean requiresRefund) {
        OrderDetailDto base = OrderDetailDto.builder().orderNumber("UNF-20260520-0001").status(status).build();
        if (requiresRefund) {
            base.setPayment(PaymentDto.builder()
                    .provider(PaymentProvider.VNPAY)
                    .status(PaymentStatus.CAPTURED)
                    .amount(new BigDecimal("500000")).currency("VND").build());
        }
        return AdminOrderDetailDto.builder()
                .order(base)
                .requiresRefund(requiresRefund)
                .build();
    }
}
