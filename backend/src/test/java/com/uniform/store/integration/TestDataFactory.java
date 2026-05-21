package com.uniform.store.integration;

import com.uniform.store.entity.Address;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.Gender;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.repository.AddressRepository;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.CategoryRepository;
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
}
