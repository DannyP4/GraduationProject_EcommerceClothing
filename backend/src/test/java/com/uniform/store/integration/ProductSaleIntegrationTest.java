package com.uniform.store.integration;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.SaleType;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductSaleIntegrationTest extends BaseIntegrationTest {

    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;

    Brand brand;
    Category category;

    @BeforeEach
    void seedCatalog() {
        brand = data.createBrand();
        category = data.createCategory();
    }

    @Test
    void storefrontDetail_activePercentSale_returnsSalePriceAndBadge() throws Exception {
        Product p = data.createProductWithSale(brand, category, new BigDecimal("250000"),
                SaleType.PERCENT, new BigDecimal("30"), null, null);
        data.createVariant(p, 10);

        mockMvc.perform(get("/products/" + p.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basePrice").value(250000))
                .andExpect(jsonPath("$.data.salePrice").value(175000))
                .andExpect(jsonPath("$.data.discountPercent").value(30))
                .andExpect(jsonPath("$.data.variants[0].price").value(250000))
                .andExpect(jsonPath("$.data.variants[0].salePrice").value(175000));
    }

    @Test
    void storefrontList_activeSale_carriesSaleFields() throws Exception {
        Product p = data.createProductWithSale(brand, category, new BigDecimal("400000"),
                SaleType.FIXED, new BigDecimal("100000"), null, null);
        data.createVariant(p, 10);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].basePrice").value(400000))
                .andExpect(jsonPath("$.data.content[0].salePrice").value(300000))
                .andExpect(jsonPath("$.data.content[0].discountPercent").value(25));
    }

    @Test
    void storefrontDetail_saleNotYetStarted_noSalePrice() throws Exception {
        Product p = data.createProductWithSale(brand, category, new BigDecimal("250000"),
                SaleType.PERCENT, new BigDecimal("30"), Instant.now().plusSeconds(86400), null);
        data.createVariant(p, 10);

        mockMvc.perform(get("/products/" + p.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basePrice").value(250000))
                .andExpect(jsonPath("$.data.salePrice").doesNotExist())
                .andExpect(jsonPath("$.data.variants[0].salePrice").doesNotExist());
    }

    @Test
    void cart_onSaleVariant_usesSalePriceAndKeepsOriginal() throws Exception {
        Product p = data.createProductWithSale(brand, category, new BigDecimal("250000"),
                SaleType.PERCENT, new BigDecimal("30"), null, null);
        ProductVariant variant = data.createVariant(p, 10);
        User buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        String jwt = data.accessTokenFor(buyer.getEmail());

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddCartItemRequest(variant.getId(), 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(175000))
                .andExpect(jsonPath("$.data.items[0].originalUnitPrice").value(250000))
                .andExpect(jsonPath("$.data.items[0].lineTotal").value(350000))
                .andExpect(jsonPath("$.data.subtotal").value(350000));
    }

    @Test
    void placeOrder_onSaleVariant_snapshotsSalePrice() throws Exception {
        Product p = data.createProductWithSale(brand, category, new BigDecimal("250000"),
                SaleType.PERCENT, new BigDecimal("30"), null, null);
        ProductVariant variant = data.createVariant(p, 10);
        User buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        String jwt = data.accessTokenFor(buyer.getEmail());
        Address address = data.createDefaultAddress(buyer);

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddCartItemRequest(variant.getId(), 2))))
                .andExpect(status().isOk());

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");

        String resp = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.order.grandTotal").value(350000))
                .andExpect(jsonPath("$.data.order.items[0].unitPrice").value(175000))
                .andExpect(jsonPath("$.data.order.items[0].originalUnitPrice").value(250000))
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(resp)
                .path("data").path("order").path("orderNumber").asText();
        Order saved = orderRepository.findByOrderNumber(orderNumber).orElseThrow();

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(saved.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getUnitPrice()).isEqualByComparingTo("175000");
        assertThat(items.get(0).getOriginalUnitPrice()).isEqualByComparingTo("250000");
        assertThat(saved.getSubtotal()).isEqualByComparingTo("350000");
    }
}
