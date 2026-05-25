package com.uniform.store.integration;

import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.bootstrap.DemoDataSeeder;
import com.uniform.store.bootstrap.SeedProperties;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataSeederIntegrationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeederIntegrationTest.class);

    @Autowired private DemoDataSeeder seeder;
    @Autowired private SeedProperties props;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private ProductImageRepository imageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;

    @Test
    void runsAndPopulatesAllTables() {
        props.setEnabled(true);
        props.setProductsCount(50);
        props.setCustomersCount(60);
        props.setOrdersCount(50);
        props.setReviewsCount(20);

        seeder.run();

        long categories = categoryRepository.count();
        long brands = brandRepository.count();
        long products = productRepository.count();
        long variants = variantRepository.count();
        long images = imageRepository.count();
        long customers = userRepository.count();
        long orders = orderRepository.count();
        long items = orderItemRepository.count();
        long payments = paymentRepository.count();
        long reviews = jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Long.class);

        log.info("SEED VERIFY -> categories={} brands={} products={} variants={} images={} " +
                "customers={} orders={} order_items={} payments={} reviews={}",
                categories, brands, products, variants, images, customers, orders, items, payments, reviews);

        assertThat(categories).isEqualTo(5);
        assertThat(brands).isEqualTo(8);
        assertThat(products).isEqualTo(50);
        assertThat(variants).isBetween(150L, 250L);
        assertThat(images).isEqualTo(50);
        assertThat(customers).isEqualTo(60);
        assertThat(orders).isEqualTo(50);
        assertThat(items).isBetween(50L, 150L);
        assertThat(payments).isBetween(30L, 50L);
        assertThat(reviews).isEqualTo(20L);
    }

    @Test
    void skipsWhenProductsAlreadyExist() {
        props.setEnabled(true);
        props.setProductsCount(50);
        seeder.run();
        long after1 = productRepository.count();

        seeder.run();
        long after2 = productRepository.count();

        assertThat(after1).isEqualTo(after2);
    }

    @Test
    void skipsWhenDisabled() {
        props.setEnabled(false);
        seeder.run();
        assertThat(productRepository.count()).isZero();
    }
}
