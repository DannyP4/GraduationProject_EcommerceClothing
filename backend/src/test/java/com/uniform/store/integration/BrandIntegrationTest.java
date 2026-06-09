package com.uniform.store.integration;

import com.uniform.store.dto.request.CreateReviewRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrandIntegrationTest extends BaseIntegrationTest {

    @Autowired ProductRepository productRepository;

    Brand brand;
    Long brandId;

    @BeforeEach
    void seed() throws Exception {
        User buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        String buyerJwt = data.accessTokenFor(buyer.getEmail());
        User other = data.createCustomer("voter@uniform.test", "Test1234");
        String otherJwt = data.accessTokenFor(other.getEmail());

        brand = data.createBrand();
        brandId = brand.getId();
        Category category = data.createCategory();

        Product p1 = data.createProduct(brand, category, new BigDecimal("250000"));
        ProductVariant v1 = data.createVariant(p1, 50);
        Product p2 = data.createProduct(brand, category, new BigDecimal("300000"));
        ProductVariant v2 = data.createVariant(p2, 50);

        // An inactive product of the same brand must not inflate productCount.
        Product inactive = data.createProduct(brand, category, new BigDecimal("400000"));
        inactive.setIsActive(false);
        productRepository.save(inactive);

        // Sold: 2 + 1 (v1) + 3 (v2) = 6 units over DELIVERED orders.
        data.createOrderWithItem(buyer, v1, 2, OrderStatus.DELIVERED, Instant.now(), PaymentProvider.COD);
        data.createOrderWithItem(other, v1, 1, OrderStatus.DELIVERED, Instant.now(), PaymentProvider.COD);
        data.createOrderWithItem(buyer, v2, 3, OrderStatus.DELIVERED, Instant.now(), PaymentProvider.COD);
        // A PENDING order must not count toward sold.
        data.createOrderWithItem(other, v2, 9, OrderStatus.PENDING, Instant.now(), PaymentProvider.COD);

        // Reviews: ratings 4, 2 (p1) and 5 (p2) -> pooled avg 11/3 = 3.7.
        submitReview(buyerJwt, p1.getId(), 4, "Good fit");
        submitReview(otherJwt, p1.getId(), 2, "Color faded");
        submitReview(buyerJwt, p2.getId(), 5, "Excellent");
    }

    private void submitReview(String jwt, Long productId, int rating, String body) throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setProductId(productId);
        req.setRating(rating);
        req.setBody(body);
        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void getBrandSummary_aggregatesActiveProductsSoldAndPooledRating() throws Exception {
        mockMvc.perform(get("/brands/" + brandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(brandId))
                .andExpect(jsonPath("$.data.name").value(brand.getName()))
                .andExpect(jsonPath("$.data.productCount").value(2))
                .andExpect(jsonPath("$.data.soldCount").value(6))
                .andExpect(jsonPath("$.data.reviewCount").value(3))
                .andExpect(jsonPath("$.data.averageRating").value(3.7));
    }

    @Test
    void getBrandSummary_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/brands/999999"))
                .andExpect(status().isNotFound());
    }
}
