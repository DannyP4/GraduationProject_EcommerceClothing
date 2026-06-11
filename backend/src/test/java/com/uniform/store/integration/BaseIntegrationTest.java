package com.uniform.store.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(resolver = InheritEnvProfilesResolver.class)
public abstract class BaseIntegrationTest {

    private static final List<String> TABLES_TO_TRUNCATE = List.of(
            "product_embeddings",
            "coupon_products",
            "coupon_categories",
            "order_coupons",
            "coupons",
            "product_views",
            "audit_log",
            "review_helpful_votes",
            "review_images",
            "reviews",
            "email_log",
            "one_time_tokens",
            "order_status_history",
            "order_items",
            "payments",
            "orders",
            "cart_items",
            "carts",
            "addresses",
            "product_images",
            "product_attributes",
            "product_translations",
            "product_variants",
            "products",
            "category_translations",
            "categories",
            "brand_translations",
            "brands",
            "users"
    );

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected TestDataFactory data;
    @Autowired protected JdbcTemplate jdbc;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : TABLES_TO_TRUNCATE) {
            jdbc.execute("TRUNCATE TABLE " + table);
        }
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        data.seedRolesIfMissing();
    }
}
