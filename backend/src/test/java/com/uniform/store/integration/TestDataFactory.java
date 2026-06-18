package com.uniform.store.integration;

import com.uniform.store.entity.Address;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Coupon;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.Gender;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.SaleType;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.enums.ShippingRegion;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.repository.AddressRepository;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.CouponRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.RoleRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class TestDataFactory {

    private static final AtomicLong COUNTER = new AtomicLong(0);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public void seedRolesIfMissing() {
        if (roleRepository.findByName(Role.CUSTOMER).isEmpty()) {
            roleRepository.save(Role.builder()
                    .name(Role.CUSTOMER).displayName("Customer")
                    .description("Standard shopper account").build());
        }
        if (roleRepository.findByName(Role.ADMIN).isEmpty()) {
            roleRepository.save(Role.builder()
                    .name(Role.ADMIN).displayName("Administrator")
                    .description("Full back-office management access").build());
        }
    }

    @Transactional
    public User createCustomer(String email, String rawPassword) {
        Role customer = roleRepository.findByName(Role.CUSTOMER).orElseThrow();
        User u = User.builder()
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName("Test " + email)
                .preferredLocale("vi")
                .role(customer)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(u);
    }

    @Transactional
    public User createAdmin(String email, String rawPassword) {
        Role admin = roleRepository.findByName(Role.ADMIN).orElseThrow();
        User u = User.builder()
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName("Admin " + email)
                .preferredLocale("vi")
                .role(admin)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(u);
    }

    public String accessTokenFor(String email) {
        return jwtUtil.generateAccessToken(email.toLowerCase());
    }

    @Transactional
    public Address createDefaultAddress(User user) {
        Address a = Address.builder()
                .user(user)
                .label("Home")
                .recipient(user.getFullName())
                .phone("0912345678")
                .line1("123 Test Street")
                .ward("Phuong 1")
                .district("Quan 1")
                .city("HCM")
                .country("VN")
                .region(ShippingRegion.SOUTH)
                .isDefault(true)
                .build();
        return addressRepository.save(a);
    }

    @Transactional
    public Brand createBrand() {
        String slug = "uniform-" + COUNTER.incrementAndGet();
        return brandRepository.save(Brand.builder()
                .slug(slug).name("UNIFORM " + slug).isActive(true).build());
    }

    @Transactional
    public Category createCategory() {
        String slug = "tees-" + COUNTER.incrementAndGet();
        return categoryRepository.save(Category.builder()
                .slug(slug).name("Tees " + slug).sortOrder(0).isActive(true).build());
    }

    @Transactional
    public Product createProduct(Brand brand, Category category, BigDecimal basePrice) {
        String slug = "essential-tee-" + COUNTER.incrementAndGet();
        return productRepository.save(Product.builder()
                .brand(brand).category(category)
                .slug(slug).name("Essential Tee " + slug)
                .description("Soft cotton t-shirt")
                .gender(Gender.UNISEX)
                .basePrice(basePrice)
                .currency("VND")
                .isActive(true)
                .build());
    }

    @Transactional
    public Product createProductWithSale(Brand brand, Category category, BigDecimal basePrice,
                                         SaleType saleType, BigDecimal saleValue,
                                         Instant saleStartsAt, Instant saleEndsAt) {
        String slug = "sale-tee-" + COUNTER.incrementAndGet();
        return productRepository.save(Product.builder()
                .brand(brand).category(category)
                .slug(slug).name("Sale Tee " + slug)
                .description("On sale")
                .gender(Gender.UNISEX)
                .basePrice(basePrice)
                .saleType(saleType)
                .saleValue(saleValue)
                .saleStartsAt(saleStartsAt)
                .saleEndsAt(saleEndsAt)
                .currency("VND")
                .isActive(true)
                .build());
    }

    @Transactional
    public ProductVariant createVariant(Product product, int stock) {
        long n = COUNTER.incrementAndGet();
        return variantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("SKU-" + n)
                .size("M").color("Black").colorHex("#000000")
                .stockQuantity(stock)
                .isActive(true)
                .build());
    }

    @Transactional
    public Order createOrderWithItem(User customer, ProductVariant variant, int qty,
                                     OrderStatus status, Instant placedAt, PaymentProvider provider) {
        long n = COUNTER.incrementAndGet();
        BigDecimal unit = variant.getProduct().getBasePrice();
        BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(qty)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal shipping = new BigDecimal("30000").setScale(4, RoundingMode.HALF_UP);
        BigDecimal grand = subtotal.add(shipping).setScale(4, RoundingMode.HALF_UP);

        Order order = orderRepository.save(Order.builder()
                .orderNumber(String.format("TEST-%05d", 10000 + n))
                .user(customer)
                .status(status)
                .subtotal(subtotal)
                .discountTotal(BigDecimal.ZERO.setScale(4))
                .shippingCost(shipping)
                .taxTotal(BigDecimal.ZERO.setScale(4))
                .grandTotal(grand)
                .currency("VND")
                .shippingRecipient(customer.getFullName())
                .shippingPhone("0900000000")
                .shippingLine1("1 Test St")
                .shippingDistrict("Quan 1")
                .shippingCity("HCM")
                .shippingCountry("VN")
                .placedAt(placedAt)
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .variant(variant)
                .productName(variant.getProduct().getName())
                .variantLabel(variant.getSize() + " / " + variant.getColor())
                .sku(variant.getSku())
                .unitPrice(unit)
                .quantity(qty)
                .lineTotal(unit.multiply(BigDecimal.valueOf(qty)).setScale(4, RoundingMode.HALF_UP))
                .build());

        if (provider != null) {
            PaymentStatus payStatus = status == OrderStatus.CANCELLED
                    ? PaymentStatus.FAILED
                    : PaymentStatus.CAPTURED;
            paymentRepository.save(Payment.builder()
                    .order(order)
                    .provider(provider)
                    .providerTxnId(provider.name().toLowerCase() + "-test-" + n)
                    .amount(grand)
                    .currency("VND")
                    .status(payStatus)
                    .paidAt(payStatus == PaymentStatus.CAPTURED ? placedAt : null)
                    .build());
        }

        return order;
    }

    @Transactional
    public Order createOrderWithItems(User customer, List<ProductVariant> variants,
                                      OrderStatus status, Instant placedAt, PaymentProvider provider) {
        long n = COUNTER.incrementAndGet();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ProductVariant v : variants) {
            subtotal = subtotal.add(v.getProduct().getBasePrice());
        }
        subtotal = subtotal.setScale(4, RoundingMode.HALF_UP);
        BigDecimal shipping = new BigDecimal("30000").setScale(4, RoundingMode.HALF_UP);
        BigDecimal grand = subtotal.add(shipping).setScale(4, RoundingMode.HALF_UP);

        Order order = orderRepository.save(Order.builder()
                .orderNumber(String.format("TESTM-%05d", 10000 + n))
                .user(customer)
                .status(status)
                .subtotal(subtotal)
                .discountTotal(BigDecimal.ZERO.setScale(4))
                .shippingCost(shipping)
                .taxTotal(BigDecimal.ZERO.setScale(4))
                .grandTotal(grand)
                .currency("VND")
                .shippingRecipient(customer.getFullName())
                .shippingPhone("0900000000")
                .shippingLine1("1 Test St")
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

        if (provider != null) {
            PaymentStatus payStatus = status == OrderStatus.CANCELLED
                    ? PaymentStatus.FAILED
                    : PaymentStatus.CAPTURED;
            paymentRepository.save(Payment.builder()
                    .order(order)
                    .provider(provider)
                    .providerTxnId(provider.name().toLowerCase() + "-testm-" + n)
                    .amount(grand)
                    .currency("VND")
                    .status(payStatus)
                    .paidAt(payStatus == PaymentStatus.CAPTURED ? placedAt : null)
                    .build());
        }

        return order;
    }

    @Transactional
    public Order createPendingOrder(User customer, ProductVariant variant, int qty,
                                    PaymentProvider provider, PaymentStatus paymentStatus, Instant placedAt) {
        long n = COUNTER.incrementAndGet();
        BigDecimal unit = variant.getProduct().getBasePrice();
        BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(qty)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal shipping = new BigDecimal("30000").setScale(4, RoundingMode.HALF_UP);
        BigDecimal grand = subtotal.add(shipping).setScale(4, RoundingMode.HALF_UP);

        Order order = orderRepository.save(Order.builder()
                .orderNumber(String.format("PEND-%05d", 10000 + n))
                .user(customer)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .discountTotal(BigDecimal.ZERO.setScale(4))
                .shippingCost(shipping)
                .taxTotal(BigDecimal.ZERO.setScale(4))
                .grandTotal(grand)
                .currency("VND")
                .shippingRecipient(customer.getFullName())
                .shippingPhone("0900000000")
                .shippingLine1("1 Test St")
                .shippingDistrict("Quan 1")
                .shippingCity("HCM")
                .shippingCountry("VN")
                .placedAt(placedAt)
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .variant(variant)
                .productName(variant.getProduct().getName())
                .variantLabel(variant.getSize() + " / " + variant.getColor())
                .sku(variant.getSku())
                .unitPrice(unit)
                .quantity(qty)
                .lineTotal(unit.multiply(BigDecimal.valueOf(qty)).setScale(4, RoundingMode.HALF_UP))
                .build());

        paymentRepository.save(Payment.builder()
                .order(order)
                .provider(provider)
                .providerTxnId(provider.name().toLowerCase() + "-pend-" + n)
                .amount(grand)
                .currency("VND")
                .status(paymentStatus)
                .build());

        return order;
    }

    @Transactional
    public Coupon saveCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Transactional
    public ProductImage createProductImage(Product product, boolean primary, String publicId) {
        long n = COUNTER.incrementAndGet();
        return productImageRepository.save(ProductImage.builder()
                .product(product)
                .url("https://test/img-" + n + ".jpg")
                .publicId(publicId)
                .altText("alt-" + n)
                .sortOrder((int) (n % 100))
                .isPrimary(primary)
                .build());
    }
}
