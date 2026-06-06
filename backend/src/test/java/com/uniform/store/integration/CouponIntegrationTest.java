package com.uniform.store.integration;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Coupon;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.CouponScope;
import com.uniform.store.enums.CouponStatus;
import com.uniform.store.enums.CouponType;
import com.uniform.store.repository.CouponRepository;
import com.uniform.store.repository.OrderCouponRepository;
import com.uniform.store.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CouponIntegrationTest extends BaseIntegrationTest {

    @Autowired CouponRepository couponRepository;
    @Autowired OrderCouponRepository orderCouponRepository;
    @Autowired OrderRepository orderRepository;

    User buyer;
    String jwt;
    Address address;
    Brand brand;
    Category category;
    ProductVariant variant;

    @BeforeEach
    void seed() {
        buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        jwt = data.accessTokenFor(buyer.getEmail());
        address = data.createDefaultAddress(buyer);
        brand = data.createBrand();
        category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        variant = data.createVariant(product, 20);
    }

    private void addToCart(Long variantId, int qty) throws Exception {
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddCartItemRequest(variantId, qty))))
                .andExpect(status().isOk());
    }

    private Coupon coupon(String code, CouponType type, String value, CouponScope scope) {
        return Coupon.builder()
                .code(code).type(type).value(new BigDecimal(value)).scope(scope)
                .status(CouponStatus.ACTIVE).usedCount(0)
                .build();
    }

    @Test
    void validate_allCoupon_returnsDiscount() throws Exception {
        addToCart(variant.getId(), 2); // 500000
        data.saveCoupon(coupon("SAVE10", CouponType.PERCENT, "10", CouponScope.ALL));

        mockMvc.perform(post("/coupons/validate")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "save10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountAmount").value(50000))
                .andExpect(jsonPath("$.data.subtotal").value(500000))
                .andExpect(jsonPath("$.data.totalAfterDiscount").value(450000));
    }

    @Test
    void validateDirect_buyNowLine_returnsDiscountWithoutCart() throws Exception {
        data.saveCoupon(coupon("SAVE10", CouponType.PERCENT, "10", CouponScope.ALL));

        mockMvc.perform(post("/coupons/validate")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "save10",
                                "variantId", variant.getId(),
                                "quantity", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountAmount").value(50000))
                .andExpect(jsonPath("$.data.subtotal").value(500000))
                .andExpect(jsonPath("$.data.totalAfterDiscount").value(450000));
    }

    @Test
    void validate_invalidCode_returns400() throws Exception {
        addToCart(variant.getId(), 1);

        mockMvc.perform(post("/coupons/validate")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "NOPE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_withCoupon_setsDiscountAndIncrementsUsage() throws Exception {
        addToCart(variant.getId(), 2);
        data.saveCoupon(coupon("SAVE10", CouponType.PERCENT, "10", CouponScope.ALL));

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");
        order.setCouponCode("SAVE10");

        String resp = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.order.discountTotal").value(50000))
                .andExpect(jsonPath("$.data.order.grandTotal").value(450000))
                .andReturn().getResponse().getContentAsString();

        assertThat(couponRepository.findByCode("SAVE10").orElseThrow().getUsedCount()).isEqualTo(1);

        String orderNumber = objectMapper.readTree(resp).path("data").path("order").path("orderNumber").asText();
        Long orderId = orderRepository.findByOrderNumber(orderNumber).orElseThrow().getId();
        assertThat(orderCouponRepository.findByOrderId(orderId)).isPresent();
    }

    @Test
    void placeOrder_thenCancel_releasesCoupon() throws Exception {
        addToCart(variant.getId(), 2);
        data.saveCoupon(coupon("SAVE10", CouponType.PERCENT, "10", CouponScope.ALL));

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");
        order.setCouponCode("SAVE10");

        String resp = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderNumber = objectMapper.readTree(resp).path("data").path("order").path("orderNumber").asText();
        Long orderId = orderRepository.findByOrderNumber(orderNumber).orElseThrow().getId();

        assertThat(couponRepository.findByCode("SAVE10").orElseThrow().getUsedCount()).isEqualTo(1);

        mockMvc.perform(post("/orders/" + orderNumber + "/cancel")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        assertThat(couponRepository.findByCode("SAVE10").orElseThrow().getUsedCount()).isEqualTo(0);
        assertThat(orderCouponRepository.findByOrderId(orderId)).isEmpty();
    }

    @Test
    void categoryScope_discountsOnlyMatchingCategory() throws Exception {
        // second product in a different category
        Category other = data.createCategory();
        Product p2 = data.createProduct(brand, other, new BigDecimal("250000"));
        ProductVariant v2 = data.createVariant(p2, 20);

        addToCart(variant.getId(), 1); // category (250000)
        addToCart(v2.getId(), 1);      // other category (250000)

        Coupon c = coupon("CAT10", CouponType.PERCENT, "10", CouponScope.CATEGORY);
        c.setCategoryIds(new LinkedHashSet<>(java.util.Set.of(category.getId())));
        data.saveCoupon(c);

        mockMvc.perform(post("/coupons/validate")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "CAT10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").value(500000))
                .andExpect(jsonPath("$.data.discountAmount").value(25000)); // only the in-category line
    }

    @Test
    void admin_createCoupon_thenListed() throws Exception {
        User admin = data.createAdmin("admin@uniform.test", "Admin1234");
        String adminJwt = data.accessTokenFor(admin.getEmail());

        mockMvc.perform(post("/admin/coupons")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "welcome", "type", "PERCENT", "value", 15, "scope", "ALL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("WELCOME"))
                .andExpect(jsonPath("$.data.scope").value("ALL"));

        mockMvc.perform(get("/admin/coupons")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void admin_createPercentOver100_returns400() throws Exception {
        User admin = data.createAdmin("admin@uniform.test", "Admin1234");
        String adminJwt = data.accessTokenFor(admin.getEmail());

        mockMvc.perform(post("/admin/coupons")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "BAD", "type", "PERCENT", "value", 150, "scope", "ALL"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_deleteUsedCoupon_blocked() throws Exception {
        data.saveCoupon(coupon("SAVE10", CouponType.PERCENT, "10", CouponScope.ALL));
        addToCart(variant.getId(), 2);

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");
        order.setCouponCode("SAVE10");
        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated());

        User admin = data.createAdmin("admin@uniform.test", "Admin1234");
        String adminJwt = data.accessTokenFor(admin.getEmail());
        Long couponId = couponRepository.findByCode("SAVE10").orElseThrow().getId();

        mockMvc.perform(delete("/admin/coupons/" + couponId)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest());
    }
}
