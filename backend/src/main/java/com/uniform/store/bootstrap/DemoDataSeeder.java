package com.uniform.store.bootstrap;

import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.OrderStatusHistory;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.Gender;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.RoleRepository;
import com.uniform.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Component
@org.springframework.core.annotation.Order(20)
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final long SEED = 42L;
    private static final String[] CATEGORY_SLUGS = {"t-shirts", "hoodies", "jackets", "pants", "accessories"};
    private static final String[] CATEGORY_NAMES = {"T-Shirts", "Hoodies", "Jackets", "Pants", "Accessories"};
    private static final String[] BRAND_NAMES = {"Vesta", "North Drift", "Atlas Studio", "Mono Lab", "Riverwear", "Six Mile", "Static Co.", "Halftone"};
    private static final String[] SIZES = {"S", "M", "L", "XL"};
    private static final String[][] COLORS = {
            {"Black", "#000000"}, {"White", "#FFFFFF"}, {"Navy", "#1F2A44"},
            {"Olive", "#5A6240"}, {"Sand", "#D6C7A1"}, {"Burgundy", "#6B1F2A"}
    };
    private static final Gender[] GENDERS = {Gender.MEN, Gender.WOMEN, Gender.UNISEX};
    private static final OrderStatus[] ORDER_STATUS_MIX = {
            OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.PAID,
            OrderStatus.PROCESSING, OrderStatus.PROCESSING,
            OrderStatus.SHIPPED, OrderStatus.SHIPPED,
            OrderStatus.DELIVERED, OrderStatus.DELIVERED, OrderStatus.DELIVERED,
            OrderStatus.CANCELLED
    };

    private final SeedProperties props;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(String... args) {
        if (!props.isEnabled()) {
            log.info("Demo seeder disabled (app.seed.enabled=false)");
            return;
        }
        Random rng = new Random(SEED);
        Faker faker = new Faker(rng);

        if (productRepository.count() == 0) {
            List<Category> categories = seedCategories();
            List<Brand> brands = seedBrands();
            List<Product> products = seedProducts(faker, rng, categories, brands);
            seedImagesAndVariants(rng, products);
            log.info("Demo seeder: seeded {} categories, {} brands, {} placeholder products.",
                    categories.size(), brands.size(), products.size());
        } else {
            log.info("Demo seeder: catalog present ({} products), layering demo transactions on it.",
                    productRepository.count());
        }

        if (orderRepository.count() == 0) {
            List<User> customers = seedCustomers(faker, rng);
            List<Product> products = productRepository.findAll();
            seedOrdersAndPayments(rng, customers, products);
            seedReviews(rng, customers, products);
            log.info("Demo seeder: seeded {} customers, {} orders, {} reviews.",
                    customers.size(), props.getOrdersCount(), props.getReviewsCount());
        } else {
            log.info("Demo seeder: orders present ({}), skipping demo transactions.", orderRepository.count());
        }
    }

    private List<Category> seedCategories() {
        List<Category> out = new ArrayList<>();
        for (int i = 0; i < CATEGORY_SLUGS.length; i++) {
            out.add(categoryRepository.save(Category.builder()
                    .slug(CATEGORY_SLUGS[i])
                    .name(CATEGORY_NAMES[i])
                    .sortOrder(0)
                    .isActive(true)
                    .build()));
        }
        return out;
    }

    private List<Brand> seedBrands() {
        List<Brand> out = new ArrayList<>();
        for (int i = 0; i < BRAND_NAMES.length; i++) {
            String name = BRAND_NAMES[i];
            out.add(brandRepository.save(Brand.builder()
                    .slug(slugify(name))
                    .name(name)
                    .websiteUrl("https://example.com/" + slugify(name))
                    .isActive(true)
                    .build()));
        }
        return out;
    }

    private List<Product> seedProducts(Faker faker, Random rng, List<Category> categories, List<Brand> brands) {
        int count = props.getProductsCount();
        List<Product> out = new ArrayList<>(count);
        Set<String> usedSlugs = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String baseName = faker.commerce().productName();
            String slug = uniqueSlug(usedSlugs, slugify(baseName), i);
            BigDecimal price = BigDecimal.valueOf(150_000L + (rng.nextInt(40) * 25_000L));
            out.add(productRepository.save(Product.builder()
                    .brand(brands.get(rng.nextInt(brands.size())))
                    .category(categories.get(rng.nextInt(categories.size())))
                    .slug(slug)
                    .name(baseName)
                    .description(faker.lorem().paragraph(3))
                    .gender(GENDERS[rng.nextInt(GENDERS.length)])
                    .basePrice(price)
                    .currency("VND")
                    .isActive(true)
                    .publishedAt(Instant.now().minus(rng.nextInt(60) + 1, ChronoUnit.DAYS))
                    .build()));
        }
        return out;
    }

    private void seedImagesAndVariants(Random rng, List<Product> products) {
        for (Product p : products) {
            String url = String.format("https://picsum.photos/seed/uniform-%d/600/800", p.getId());
            imageRepository.save(ProductImage.builder()
                    .product(p)
                    .url(url)
                    .altText(p.getName())
                    .sortOrder(0)
                    .isPrimary(true)
                    .build());

            int variantCount = 3 + rng.nextInt(3);
            Set<String> combos = new HashSet<>();
            int safety = 0;
            while (combos.size() < variantCount && safety++ < 30) {
                String size = SIZES[rng.nextInt(SIZES.length)];
                String[] color = COLORS[rng.nextInt(COLORS.length)];
                String key = size + "|" + color[0];
                if (combos.add(key)) {
                    variantRepository.save(ProductVariant.builder()
                            .product(p)
                            .sku(String.format("UNI-%d-%s-%s", p.getId(), size, color[0].substring(0, 2).toUpperCase()))
                            .size(size)
                            .color(color[0])
                            .colorHex(color[1])
                            .stockQuantity(5 + rng.nextInt(45))
                            .isActive(true)
                            .build());
                }
            }
        }
    }

    private List<User> seedCustomers(Faker faker, Random rng) {
        Role customerRole = roleRepository.findByName(Role.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Customer role missing; V1 migration must run first"));
        String hash = passwordEncoder.encode("Password123");
        List<User> out = new ArrayList<>(props.getCustomersCount());
        for (int i = 1; i <= props.getCustomersCount(); i++) {
            String email = "demo" + i + "@uniform.local";
            if (userRepository.existsByEmail(email)) continue;
            out.add(userRepository.save(User.builder()
                    .role(customerRole)
                    .email(email)
                    .passwordHash(hash)
                    .fullName(faker.name().fullName())
                    .phone("09" + (10_000_000 + rng.nextInt(90_000_000)))
                    .preferredLocale(rng.nextBoolean() ? "vi" : "en")
                    .status(UserStatus.ACTIVE)
                    .build()));
        }
        return out;
    }

    private void seedOrdersAndPayments(Random rng, List<User> customers, List<Product> products) {
        if (customers.isEmpty()) return;
        List<ProductVariant> allVariants = variantRepository.findAll();
        if (allVariants.isEmpty()) return;

        for (int i = 0; i < props.getOrdersCount(); i++) {
            User customer = customers.get(rng.nextInt(customers.size()));
            OrderStatus status = ORDER_STATUS_MIX[i % ORDER_STATUS_MIX.length];
            Instant placedAt = Instant.now().minus(rng.nextInt(45) + 1, ChronoUnit.DAYS);

            int itemCount = 1 + rng.nextInt(3);
            List<ProductVariant> picked = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            while (picked.size() < itemCount) {
                ProductVariant v = allVariants.get(rng.nextInt(allVariants.size()));
                if (seen.add(v.getId())) picked.add(v);
            }

            BigDecimal subtotal = BigDecimal.ZERO;
            List<int[]> itemQuantities = new ArrayList<>();
            for (ProductVariant v : picked) {
                int qty = 1 + rng.nextInt(2);
                itemQuantities.add(new int[]{qty});
                subtotal = subtotal.add(v.getProduct().getBasePrice().multiply(BigDecimal.valueOf(qty)));
            }
            BigDecimal shipping = BigDecimal.valueOf(30_000);
            BigDecimal grand = subtotal.add(shipping).setScale(4, RoundingMode.HALF_UP);

            Order order = orderRepository.save(Order.builder()
                    .orderNumber(String.format("DEMO-%05d", 10001 + i))
                    .user(customer)
                    .status(status)
                    .subtotal(subtotal.setScale(4, RoundingMode.HALF_UP))
                    .discountTotal(BigDecimal.ZERO)
                    .shippingCost(shipping.setScale(4, RoundingMode.HALF_UP))
                    .taxTotal(BigDecimal.ZERO)
                    .grandTotal(grand)
                    .currency("VND")
                    .shippingRecipient(customer.getFullName())
                    .shippingPhone(customer.getPhone())
                    .shippingLine1((100 + rng.nextInt(900)) + " Demo Street")
                    .shippingWard("Phuong " + (1 + rng.nextInt(15)))
                    .shippingDistrict("Quan " + (1 + rng.nextInt(12)))
                    .shippingCity(rng.nextBoolean() ? "HCM" : "Ha Noi")
                    .shippingCountry("VN")
                    .placedAt(placedAt)
                    .build());

            for (int j = 0; j < picked.size(); j++) {
                ProductVariant v = picked.get(j);
                int qty = itemQuantities.get(j)[0];
                BigDecimal unit = v.getProduct().getBasePrice();
                orderItemRepository.save(OrderItem.builder()
                        .order(order)
                        .variant(v)
                        .productName(v.getProduct().getName())
                        .variantLabel(v.getSize() + " / " + v.getColor())
                        .sku(v.getSku())
                        .unitPrice(unit)
                        .quantity(qty)
                        .lineTotal(unit.multiply(BigDecimal.valueOf(qty)).setScale(4, RoundingMode.HALF_UP))
                        .build());
            }

            orderHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .status(status)
                    .note("Seeded demo order")
                    .build());

            if (status != OrderStatus.PENDING) {
                PaymentProvider provider = switch (rng.nextInt(3)) {
                    case 0 -> PaymentProvider.COD;
                    case 1 -> PaymentProvider.VNPAY;
                    default -> PaymentProvider.STRIPE;
                };
                PaymentStatus paymentStatus = status == OrderStatus.CANCELLED ? PaymentStatus.FAILED : PaymentStatus.CAPTURED;
                paymentRepository.save(Payment.builder()
                        .order(order)
                        .provider(provider)
                        .providerTxnId(provider.name().toLowerCase() + "-demo-" + order.getId())
                        .amount(grand)
                        .currency("VND")
                        .status(paymentStatus)
                        .paidAt(paymentStatus == PaymentStatus.CAPTURED ? placedAt.plus(2, ChronoUnit.MINUTES) : null)
                        .build());
            }
        }
    }

    private void seedReviews(Random rng, List<User> customers, List<Product> products) {
        if (customers.isEmpty() || products.isEmpty()) return;
        String[] statuses = {"APPROVED", "APPROVED", "APPROVED", "APPROVED", "PENDING"};
        String[] titles = {"Solid fit", "Great fabric", "Runs small", "Exactly as pictured", "Will reorder", "Decent for the price"};
        String[] bodies = {
                "Material feels good, washed twice without fading.",
                "Stitching is tidy, fits true to size.",
                "Shipping was fast, packaging clean.",
                "Color matches the listing photo well.",
                "Comfortable for daily wear; would buy in another color."
        };
        for (int i = 0; i < props.getReviewsCount(); i++) {
            User u = customers.get(rng.nextInt(customers.size()));
            Product p = products.get(rng.nextInt(products.size()));
            int rating = 3 + rng.nextInt(3);
            String status = statuses[rng.nextInt(statuses.length)];
            jdbc.update("""
                    INSERT INTO reviews (user_id, product_id, rating, title, body, verified_purchase, helpful_count, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    u.getId(), p.getId(), rating,
                    titles[rng.nextInt(titles.length)],
                    bodies[rng.nextInt(bodies.length)],
                    rng.nextBoolean(), rng.nextInt(20), status);
        }
    }

    private static String slugify(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private static String uniqueSlug(Set<String> used, String base, int idx) {
        String candidate = base;
        if (candidate.isBlank()) candidate = "product";
        if (used.add(candidate)) return candidate;
        candidate = base + "-" + (idx + 1);
        used.add(candidate);
        return candidate;
    }
}
