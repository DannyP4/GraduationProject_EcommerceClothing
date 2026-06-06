package com.uniform.store.integration;

import com.uniform.store.dto.request.CreateReviewRequest;
import com.uniform.store.dto.request.UpdateReviewRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewIntegrationTest extends BaseIntegrationTest {

    @Autowired ReviewRepository reviewRepository;

    User buyer;
    String buyerJwt;
    User otherBuyer;
    String otherJwt;
    User admin;
    String adminJwt;

    Product product;        // purchased + delivered by both buyers
    Product otherProduct;   // not purchased by buyer
    ProductVariant variant;

    @BeforeEach
    void seed() {
        buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        buyerJwt = data.accessTokenFor(buyer.getEmail());
        otherBuyer = data.createCustomer("voter@uniform.test", "Test1234");
        otherJwt = data.accessTokenFor(otherBuyer.getEmail());
        admin = data.createAdmin("admin@uniform.test", "Admin1234");
        adminJwt = data.accessTokenFor(admin.getEmail());

        Brand brand = data.createBrand();
        Category category = data.createCategory();
        product = data.createProduct(brand, category, new BigDecimal("250000"));
        variant = data.createVariant(product, 20);
        otherProduct = data.createProduct(brand, category, new BigDecimal("300000"));

        data.createOrderWithItem(buyer, variant, 1, OrderStatus.DELIVERED, Instant.now(), PaymentProvider.COD);
        data.createOrderWithItem(otherBuyer, variant, 1, OrderStatus.DELIVERED, Instant.now(), PaymentProvider.COD);
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
    void create_withoutPurchase_returns400() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setProductId(otherProduct.getId());
        req.setRating(5);
        req.setBody("Looks nice");

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    void create_withVerifiedPurchase_returns200AndAutoApproved() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setProductId(product.getId());
        req.setRating(5);
        req.setBody("Excellent quality, fits true to size");

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.verifiedPurchase").value(true))
                .andExpect(jsonPath("$.data.mine").value(true))
                .andExpect(jsonPath("$.data.variantColor").value("Black"))
                .andExpect(jsonPath("$.data.variantSize").value("M"))
                .andExpect(jsonPath("$.data.variantColorHex").value("#000000"));

        assertThat(reviewRepository.findAll().get(0).getStatus()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    void create_duplicate_returns400() throws Exception {
        submitReview(buyerJwt, product.getId(), 5, "First review");

        CreateReviewRequest req = new CreateReviewRequest();
        req.setProductId(product.getId());
        req.setRating(4);
        req.setBody("Second review attempt");

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertThat(reviewRepository.count()).isEqualTo(1);
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setProductId(product.getId());
        req.setRating(5);
        req.setBody("Anonymous review");

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicList_showsApprovedOnly_hidesRejected() throws Exception {
        submitReview(buyerJwt, product.getId(), 5, "Great tee");
        Long reviewId = reviewRepository.findAll().get(0).getId();

        mockMvc.perform(get("/products/" + product.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(post("/admin/reviews/" + reviewId + "/reject")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get("/products/" + product.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void productDetail_reflectsAverageAndCount() throws Exception {
        submitReview(buyerJwt, product.getId(), 4, "Good");
        submitReview(otherJwt, product.getId(), 2, "Meh");

        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(2))
                .andExpect(jsonPath("$.data.averageRating").value(3.0))
                .andExpect(jsonPath("$.data.soldCount").value(2));
    }

    @Test
    void productDetail_soldCount_excludesNonSaleStatuses() throws Exception {
        // Seed already has 2 DELIVERED units; a PENDING order must not inflate the count.
        data.createOrderWithItem(buyer, variant, 5, OrderStatus.PENDING, Instant.now(), PaymentProvider.COD);

        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.soldCount").value(2));
    }

    @Test
    void helpful_voteAndSelfVoteBlockedAndUnvote() throws Exception {
        submitReview(buyerJwt, product.getId(), 5, "Great tee");
        Long reviewId = reviewRepository.findAll().get(0).getId();

        mockMvc.perform(post("/reviews/" + reviewId + "/helpful")
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.helpfulCount").value(1))
                .andExpect(jsonPath("$.data.voted").value(true));

        mockMvc.perform(post("/reviews/" + reviewId + "/helpful")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/reviews/" + reviewId + "/helpful")
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.helpfulCount").value(0))
                .andExpect(jsonPath("$.data.voted").value(false));
    }

    @Test
    void eligibility_reflectsPurchaseAndExistingReview() throws Exception {
        mockMvc.perform(get("/reviews/eligibility?productId=" + product.getId())
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canReview").value(true));

        mockMvc.perform(get("/reviews/eligibility?productId=" + otherProduct.getId())
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canReview").value(false))
                .andExpect(jsonPath("$.data.reason").value("NOT_PURCHASED"));

        submitReview(buyerJwt, product.getId(), 5, "Great tee");

        mockMvc.perform(get("/reviews/eligibility?productId=" + product.getId())
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canReview").value(false))
                .andExpect(jsonPath("$.data.reason").value("ALREADY_REVIEWED"));
    }

    @Test
    void updateOwnReview_changesRating() throws Exception {
        submitReview(buyerJwt, product.getId(), 5, "Great");
        Long reviewId = reviewRepository.findAll().get(0).getId();

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setRating(3);
        req.setBody("Changed my mind after a wash");

        mockMvc.perform(put("/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(3));

        assertThat(reviewRepository.findById(reviewId).orElseThrow().getRating()).isEqualTo(3);
    }

    @Test
    void update_notOwner_returns404() throws Exception {
        submitReview(buyerJwt, product.getId(), 5, "Great");
        Long reviewId = reviewRepository.findAll().get(0).getId();

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setRating(1);
        req.setBody("Trying to edit someone else review");

        mockMvc.perform(put("/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + otherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOwnReview_removesIt() throws Exception {
        submitReview(buyerJwt, product.getId(), 5, "Great");
        Long reviewId = reviewRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk());

        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    void adminList_requiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin/reviews"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/reviews")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/reviews")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());
    }

    @Test
    void adminList_filterByStatus() throws Exception {
        submitReview(buyerJwt, product.getId(), 5, "Great tee");

        mockMvc.perform(get("/admin/reviews?status=APPROVED")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].productName").exists())
                .andExpect(jsonPath("$.data.content[0].userEmail").value("buyer@uniform.test"));

        mockMvc.perform(get("/admin/reviews?status=PENDING")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void adminGetById_returnsFullReview_andGuards() throws Exception {
        submitReview(buyerJwt, product.getId(), 4, "Detailed body for the modal view");
        Long reviewId = reviewRepository.findAll().get(0).getId();

        mockMvc.perform(get("/admin/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reviewId))
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.body").value("Detailed body for the modal view"))
                .andExpect(jsonPath("$.data.productName").exists())
                .andExpect(jsonPath("$.data.userEmail").value("buyer@uniform.test"));

        mockMvc.perform(get("/admin/reviews/999999")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/admin/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isForbidden());
    }
}
