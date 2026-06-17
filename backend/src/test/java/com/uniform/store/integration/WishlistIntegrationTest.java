package com.uniform.store.integration;

import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.User;
import com.uniform.store.entity.Wishlist;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishlistIntegrationTest extends BaseIntegrationTest {

    @Autowired WishlistRepository wishlistRepository;
    @Autowired ProductRepository productRepository;

    User buyer;
    String buyerJwt;
    User other;
    String otherJwt;

    Product activeProduct;
    Product inactiveProduct;
    Product deletedProduct;

    @BeforeEach
    void seed() {
        buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        buyerJwt = data.accessTokenFor(buyer.getEmail());
        other = data.createCustomer("other@uniform.test", "Test1234");
        otherJwt = data.accessTokenFor(other.getEmail());

        Brand brand = data.createBrand();
        Category category = data.createCategory();
        activeProduct = data.createProduct(brand, category, new BigDecimal("250000"));
        data.createProductImage(activeProduct, true, "pid-primary");

        inactiveProduct = data.createProduct(brand, category, new BigDecimal("150000"));
        inactiveProduct.setIsActive(false);
        productRepository.save(inactiveProduct);

        deletedProduct = data.createProduct(brand, category, new BigDecimal("180000"));
        deletedProduct.setDeletedAt(Instant.now());
        productRepository.save(deletedProduct);
    }

    @Test
    void toggle_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/wishlist/" + activeProduct.getId() + "/toggle"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void toggle_addsThenRemoves() throws Exception {
        mockMvc.perform(post("/wishlist/" + activeProduct.getId() + "/toggle")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wishlisted").value(true))
                .andExpect(jsonPath("$.data.productId").value(activeProduct.getId().intValue()));
        assertThat(wishlistRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/wishlist/" + activeProduct.getId() + "/toggle")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wishlisted").value(false));
        assertThat(wishlistRepository.count()).isZero();
    }

    @Test
    void toggle_unknownProduct_returns404() throws Exception {
        mockMvc.perform(post("/wishlist/999999/toggle")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isNotFound());
        assertThat(wishlistRepository.count()).isZero();
    }

    @Test
    void toggle_inactiveProduct_returns400() throws Exception {
        mockMvc.perform(post("/wishlist/" + inactiveProduct.getId() + "/toggle")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isBadRequest());
        assertThat(wishlistRepository.count()).isZero();
    }

    @Test
    void list_returnsOnlyAvailableProductCards() throws Exception {
        wishlistRepository.save(Wishlist.builder().user(buyer).product(activeProduct).build());
        wishlistRepository.save(Wishlist.builder().user(buyer).product(inactiveProduct).build());
        wishlistRepository.save(Wishlist.builder().user(buyer).product(deletedProduct).build());

        mockMvc.perform(get("/wishlist")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(activeProduct.getId().intValue()));
    }

    @Test
    void ids_returnsAllWishlistedProductIds() throws Exception {
        wishlistRepository.save(Wishlist.builder().user(buyer).product(activeProduct).build());
        wishlistRepository.save(Wishlist.builder().user(buyer).product(inactiveProduct).build());

        mockMvc.perform(get("/wishlist/ids")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void delete_removesItem_idempotent() throws Exception {
        wishlistRepository.save(Wishlist.builder().user(buyer).product(activeProduct).build());

        mockMvc.perform(delete("/wishlist/" + activeProduct.getId())
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk());
        assertThat(wishlistRepository.count()).isZero();

        mockMvc.perform(delete("/wishlist/" + activeProduct.getId())
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk());
        assertThat(wishlistRepository.count()).isZero();
    }

    @Test
    void list_isolatedPerUser() throws Exception {
        wishlistRepository.save(Wishlist.builder().user(buyer).product(activeProduct).build());

        mockMvc.perform(get("/wishlist")
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void clear_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/wishlist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clear_removesAllForUser() throws Exception {
        wishlistRepository.save(Wishlist.builder().user(buyer).product(activeProduct).build());
        wishlistRepository.save(Wishlist.builder().user(buyer).product(inactiveProduct).build());
        assertThat(wishlistRepository.count()).isEqualTo(2);

        mockMvc.perform(delete("/wishlist")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk());
        assertThat(wishlistRepository.count()).isZero();
    }

    @Test
    void clear_isolatedPerUser() throws Exception {
        wishlistRepository.save(Wishlist.builder().user(buyer).product(activeProduct).build());
        wishlistRepository.save(Wishlist.builder().user(other).product(activeProduct).build());

        mockMvc.perform(delete("/wishlist")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk());

        assertThat(wishlistRepository.count()).isEqualTo(1);
    }
}
