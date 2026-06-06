package com.uniform.store.integration;

import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Review;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminNotificationIntegrationTest extends BaseIntegrationTest {

    @Autowired ReviewRepository reviewRepository;

    User customer;
    User admin;
    String customerJwt;
    String adminJwt;

    @BeforeEach
    void seed() {
        customer = data.createCustomer("notif-buyer@uniform.test", "Test1234");
        admin = data.createAdmin("notif-admin@uniform.test", "Admin1234");
        customerJwt = data.accessTokenFor(customer.getEmail());
        adminJwt = data.accessTokenFor(admin.getEmail());

        Brand brand = data.createBrand();
        Category category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        ProductVariant lowStock = data.createVariant(product, 2);

        data.createOrderWithItem(customer, lowStock, 1, OrderStatus.PENDING, Instant.now(), null);

        reviewRepository.save(Review.builder()
                .user(customer).product(product).rating(2)
                .title("meh").body("not great")
                .verifiedPurchase(true).status(ReviewStatus.APPROVED).build());
    }

    @Test
    void feed_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void feed_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(get("/admin/notifications")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void feed_withAdminJwt_returnsOrderStockAndReviewItems() throws Exception {
        mockMvc.perform(get("/admin/notifications")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.type=='ORDER')]").exists())
                .andExpect(jsonPath("$.data[?(@.type=='STOCK')]").exists())
                .andExpect(jsonPath("$.data[?(@.type=='REVIEW')]").exists());
    }
}
