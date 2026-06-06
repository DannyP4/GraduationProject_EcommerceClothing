package com.uniform.store.service.impl;

import com.uniform.store.dto.fx.FxQuote;
import com.uniform.store.dto.request.DirectOrderRequest;
import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.response.PlaceOrderResponse;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Cart;
import com.uniform.store.entity.CartItem;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.Gender;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.mapper.OrderMapper;
import com.uniform.store.repository.AddressRepository;
import com.uniform.store.repository.CartItemRepository;
import com.uniform.store.repository.CartRepository;
import com.uniform.store.repository.OrderCouponRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.CouponService;
import com.uniform.store.service.FxService;
import com.uniform.store.service.PricingService;
import com.uniform.store.service.StripeService;
import com.uniform.store.service.ShippingService;
import com.uniform.store.service.VnpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock OrderStatusHistoryRepository statusHistoryRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock OrderNumberGenerator orderNumberGenerator;
    @Mock FxService fxService;
    @Mock VnpayService vnpayService;
    @Mock StripeService stripeService;
    @Mock OrderMapper orderMapper;
    @Mock CouponService couponService;
    @Mock OrderCouponRepository orderCouponRepository;
    @Mock ShippingService shippingService;

    OrderServiceImpl orderService;

    User user;
    Address address;
    Cart cart;
    Product product;
    ProductVariant variant;
    CartItem cartItem;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                userRepository, addressRepository, cartRepository, cartItemRepository,
                variantRepository, orderRepository, orderItemRepository,
                statusHistoryRepository, paymentRepository, orderNumberGenerator,
                fxService, vnpayService, stripeService, orderMapper, new PricingService(),
                couponService, orderCouponRepository, shippingService);

        lenient().when(shippingService.fee(any(), any())).thenReturn(BigDecimal.ZERO);

        user = User.builder()
                .email("buyer@uniform.test")
                .passwordHash("hash")
                .fullName("Buyer")
                .preferredLocale("vi")
                .role(Role.builder().name("customer").displayName("Customer").build())
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        address = Address.builder()
                .user(user).recipient("Buyer").phone("0912345678")
                .line1("123 Main").district("Q1").city("HCM").country("VN")
                .isDefault(true).build();
        address.setId(2L);

        cart = Cart.builder().user(user).build();
        cart.setId(10L);

        Brand brand = Brand.builder().slug("uniform").name("UNIFORM").build();
        brand.setId(100L);
        Category category = Category.builder().slug("tee").name("Tee").build();
        category.setId(200L);

        product = Product.builder()
                .brand(brand).category(category)
                .slug("essential-tee").name("Essential Tee")
                .gender(Gender.UNISEX)
                .basePrice(new BigDecimal("250000"))
                .currency("VND")
                .isActive(true)
                .build();
        product.setId(300L);

        variant = ProductVariant.builder()
                .product(product)
                .sku("ET-M-BLK").size("M").color("Black").colorHex("#000000")
                .stockQuantity(20)
                .isActive(true)
                .build();
        variant.setId(400L);

        cartItem = CartItem.builder().cart(cart).variant(variant).quantity(2).build();
        cartItem.setId(50L);
    }

    @Test
    void placeOrder_cod_decrementsStockAndClearsCartAndCreatesCodPayment() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of(cartItem));
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));
        when(orderNumberGenerator.next()).thenReturn("ORD-20260514-ABC123");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(700L);
            return o;
        });

        PlaceOrderResponse response = orderService.placeOrder(
                "buyer@uniform.test",
                buildRequest("COD"),
                "127.0.0.1");

        assertThat(variant.getStockQuantity()).as("stock 20 - 2 = 18").isEqualTo(18);
        verify(cartItemRepository).deleteAllByCartId(10L);

        ArgumentCaptor<Order> orderCap = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCap.capture());
        assertThat(orderCap.getValue().getGrandTotal())
                .as("250k * 2 = 500k VND").isEqualByComparingTo("500000");
        assertThat(orderCap.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);

        ArgumentCaptor<Payment> paymentCap = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCap.capture());
        assertThat(paymentCap.getValue().getProvider()).isEqualTo(PaymentProvider.COD);
        assertThat(paymentCap.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(paymentCap.getValue().getCurrency()).isEqualTo("VND");

        assertThat(response.getRedirectUrl()).as("COD has no redirect").isNull();
    }

    @Test
    void placeOrder_vnpay_returnsRedirectUrlAndCreatesVnpayPayment() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of(cartItem));
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));
        when(orderNumberGenerator.next()).thenReturn("ORD-20260514-VNP001");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(701L);
            return o;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
        when(vnpayService.buildPaymentUrl(eq("ORD-20260514-VNP001"), any(), eq("127.0.0.1")))
                .thenReturn("https://vnpay.test/pay?ref=ORD-20260514-VNP001&signed=xxx");

        PlaceOrderResponse response = orderService.placeOrder(
                "buyer@uniform.test",
                buildRequest("VNPAY"),
                "127.0.0.1");

        assertThat(response.getRedirectUrl()).as("VNPAY checkout URL").contains("vnpay.test");
        assertThat(response.getPaymentRef()).isEqualTo("ORD-20260514-VNP001");

        ArgumentCaptor<Payment> paymentCap = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCap.capture());
        assertThat(paymentCap.getValue().getProvider()).isEqualTo(PaymentProvider.VNPAY);
        assertThat(paymentCap.getValue().getProviderTxnId()).isEqualTo("ORD-20260514-VNP001");
    }

    @Test
    void placeOrder_stripe_savesFxSnapshotAndUsdAmount() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of(cartItem));
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));
        when(orderNumberGenerator.next()).thenReturn("ORD-20260514-STR001");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(702L);
            return o;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        FxQuote quote = new FxQuote(
                new BigDecimal("500000"), "VND",
                new BigDecimal("19.25"), "USD",
                new BigDecimal("0.0000385"), "TEST", Instant.now());
        when(fxService.quoteVndToUsd(any())).thenReturn(quote);
        when(stripeService.createCheckoutSession(eq("ORD-20260514-STR001"), eq(1925L), eq("USD")))
                .thenReturn(new StripeService.StripeSession("cs_test_abc", "https://checkout.stripe.test/cs_test_abc"));

        PlaceOrderResponse response = orderService.placeOrder(
                "buyer@uniform.test",
                buildRequest("STRIPE"),
                "127.0.0.1");

        assertThat(response.getRedirectUrl()).isEqualTo("https://checkout.stripe.test/cs_test_abc");

        ArgumentCaptor<Payment> paymentCap = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCap.capture());
        Payment saved = paymentCap.getValue();
        assertThat(saved.getProvider()).isEqualTo(PaymentProvider.STRIPE);
        assertThat(saved.getCurrency()).as("payment row is USD for Stripe").isEqualTo("USD");
        assertThat(saved.getAmount()).isEqualByComparingTo("19.25");
        assertThat(saved.getProviderTxnId()).isEqualTo("cs_test_abc");
        assertThat(saved.getRawRequest()).as("FX snapshot embedded")
                .containsEntry("originalCurrency", "VND")
                .containsEntry("convertedCurrency", "USD")
                .containsEntry("checkoutSessionId", "cs_test_abc");
    }

    @Test
    void placeOrder_emptyCart_throwsBadRequest() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.placeOrder(
                "buyer@uniform.test", buildRequest("COD"), "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cart is empty");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_insufficientStock_throwsBadRequest() {
        variant.setStockQuantity(1);
        cartItem.setQuantity(3);

        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of(cartItem));
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));

        assertThatThrownBy(() -> orderService.placeOrder(
                "buyer@uniform.test", buildRequest("COD"), "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Items unavailable");

        assertThat(variant.getStockQuantity()).as("stock untouched on failure").isEqualTo(1);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_pendingOrder_restoresStockAndFlipsStatuses() {
        Order order = Order.builder()
                .orderNumber("ORD-20260514-CAN001")
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("500000"))
                .grandTotal(new BigDecimal("500000"))
                .currency("VND")
                .shippingRecipient("Buyer").shippingPhone("0912345678")
                .shippingLine1("123 Main").shippingDistrict("Q1").shippingCity("HCM")
                .shippingCountry("VN")
                .placedAt(Instant.now())
                .build();
        order.setId(700L);

        OrderItem orderItem = OrderItem.builder()
                .order(order).variant(variant)
                .productName(product.getName()).variantLabel("M / Black").sku(variant.getSku())
                .unitPrice(new BigDecimal("250000")).quantity(2).lineTotal(new BigDecimal("500000"))
                .build();

        Payment payment = Payment.builder()
                .order(order).provider(PaymentProvider.COD)
                .amount(new BigDecimal("500000")).currency("VND")
                .status(PaymentStatus.PENDING).build();
        payment.setId(800L);

        variant.setStockQuantity(18);

        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(orderRepository.findByOrderNumberAndUserId("ORD-20260514-CAN001", 1L))
                .thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(700L)).thenReturn(List.of(orderItem));
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(700L)).thenReturn(Optional.of(payment));

        orderService.cancelOrder("buyer@uniform.test", "ORD-20260514-CAN001");

        assertThat(variant.getStockQuantity()).as("stock 18 + 2 restored = 20").isEqualTo(20);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getStatus()).as("payment moves to FAILED (closest terminal)")
                .isEqualTo(PaymentStatus.FAILED);

        verify(statusHistoryRepository, times(1)).save(any());
    }

    @Test
    void cancelOrder_nonPendingStatus_throwsBadRequest() {
        Order order = Order.builder()
                .orderNumber("ORD-X")
                .user(user)
                .status(OrderStatus.PAID)
                .subtotal(BigDecimal.ZERO).grandTotal(BigDecimal.ZERO).currency("VND")
                .shippingRecipient("X").shippingPhone("X").shippingLine1("X")
                .shippingDistrict("X").shippingCity("X")
                .placedAt(Instant.now()).build();
        order.setId(701L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(orderRepository.findByOrderNumberAndUserId("ORD-X", 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("buyer@uniform.test", "ORD-X"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PENDING orders can be cancelled");

        verify(variantRepository, never()).findAllByIdInWithProductForUpdate(anyCollection());
    }

    @Test
    void placeDirectOrder_cod_decrementsStockAndCreatesOrderWithoutTouchingCart() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(address));
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));
        when(orderNumberGenerator.next()).thenReturn("ORD-20260606-DIR001");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(710L);
            return o;
        });

        PlaceOrderResponse response = orderService.placeDirectOrder(
                "buyer@uniform.test", buildDirectRequest(3, "COD"), "127.0.0.1");

        assertThat(variant.getStockQuantity()).as("stock 20 - 3 = 17").isEqualTo(17);

        ArgumentCaptor<Order> orderCap = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCap.capture());
        assertThat(orderCap.getValue().getGrandTotal()).as("250k * 3 = 750k").isEqualByComparingTo("750000");
        assertThat(orderCap.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);

        verify(paymentRepository).save(any(Payment.class));
        verify(cartRepository, never()).findByUserId(any());
        verify(cartItemRepository, never()).deleteAllByCartId(any());
        assertThat(response.getRedirectUrl()).isNull();
    }

    @Test
    void placeDirectOrder_insufficientStock_throwsBadRequest() {
        variant.setStockQuantity(1);
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(address));
        when(variantRepository.findAllByIdInWithProductForUpdate(anyCollection())).thenReturn(List.of(variant));

        assertThatThrownBy(() -> orderService.placeDirectOrder(
                "buyer@uniform.test", buildDirectRequest(5, "COD"), "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Items unavailable");

        assertThat(variant.getStockQuantity()).as("stock untouched on failure").isEqualTo(1);
        verify(orderRepository, never()).save(any());
    }

    private static PlaceOrderRequest buildRequest(String method) {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setAddressId(2L);
        req.setPaymentMethod(method);
        req.setNotes(null);
        return req;
    }

    private static DirectOrderRequest buildDirectRequest(int quantity, String method) {
        DirectOrderRequest req = new DirectOrderRequest();
        req.setVariantId(400L);
        req.setQuantity(quantity);
        req.setAddressId(2L);
        req.setPaymentMethod(method);
        return req;
    }
}
