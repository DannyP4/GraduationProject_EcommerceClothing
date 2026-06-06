package com.uniform.store.integration;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.DirectOrderRequest;
import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.repository.CartItemRepository;
import com.uniform.store.repository.CartRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartOrderFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired ProductVariantRepository variantRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired CartRepository cartRepository;
    @Autowired CartItemRepository cartItemRepository;

    User buyer;
    String jwt;
    Address address;
    ProductVariant variant;

    @BeforeEach
    void seedBuyerAndCatalog() {
        buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        jwt = data.accessTokenFor(buyer.getEmail());
        address = data.createDefaultAddress(buyer);
        Brand brand = data.createBrand();
        Category category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        variant = data.createVariant(product, 20);
    }

    @Test
    void addToCart_thenPlaceCodOrder_decrementsStockAndCreatesPendingPayment() throws Exception {
        AddCartItemRequest addReq = new AddCartItemRequest(variant.getId(), 2);
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");

        String resp = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.order.status").value("PENDING"))
                .andExpect(jsonPath("$.data.order.grandTotal").value(500000))
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(resp)
                .path("data").path("order").path("orderNumber").asText();

        ProductVariant after = variantRepository.findById(variant.getId()).orElseThrow();
        assertThat(after.getStockQuantity()).as("20 - 2 = 18 after COD order").isEqualTo(18);

        var savedOrder = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);

        var payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(savedOrder.getId()).orElseThrow();
        assertThat(payment.getProvider()).isEqualTo(PaymentProvider.COD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCurrency()).isEqualTo("VND");
    }

    @Test
    void placeOrder_emptyCart_returns400() throws Exception {
        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeAndCancelCodOrder_restoresStock() throws Exception {
        AddCartItemRequest addReq = new AddCartItemRequest(variant.getId(), 3);
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");

        String resp = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(resp)
                .path("data").path("order").path("orderNumber").asText();

        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity())
                .as("20 - 3 = 17 after placement").isEqualTo(17);

        mockMvc.perform(post("/orders/" + orderNumber + "/cancel")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity())
                .as("stock restored to 20 after cancel").isEqualTo(20);
    }

    @Test
    void buyNow_cod_createsOrderAndLeavesCartUntouched() throws Exception {
        AddCartItemRequest addReq = new AddCartItemRequest(variant.getId(), 2);
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        DirectOrderRequest direct = new DirectOrderRequest();
        direct.setVariantId(variant.getId());
        direct.setQuantity(3);
        direct.setAddressId(address.getId());
        direct.setPaymentMethod("COD");

        String resp = mockMvc.perform(post("/orders/direct")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(direct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.order.status").value("PENDING"))
                .andExpect(jsonPath("$.data.order.grandTotal").value(750000))
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(resp)
                .path("data").path("order").path("orderNumber").asText();

        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity())
                .as("20 - 3 = 17 after Buy Now").isEqualTo(17);

        var savedOrder = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        var payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(savedOrder.getId()).orElseThrow();
        assertThat(payment.getProvider()).isEqualTo(PaymentProvider.COD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        var cart = cartRepository.findByUserId(buyer.getId()).orElseThrow();
        assertThat(cartItemRepository.findByCartIdOrderByIdAsc(cart.getId()))
                .as("cart still holds the original item; Buy Now did not clear it").hasSize(1);
    }

    @Test
    void buyNow_insufficientStock_returns400() throws Exception {
        DirectOrderRequest direct = new DirectOrderRequest();
        direct.setVariantId(variant.getId());
        direct.setQuantity(999);
        direct.setAddressId(address.getId());
        direct.setPaymentMethod("COD");

        mockMvc.perform(post("/orders/direct")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(direct)))
                .andExpect(status().isBadRequest());

        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity())
                .as("stock untouched on failure").isEqualTo(20);
    }

    @Test
    void buyNow_withoutAuth_returns401() throws Exception {
        DirectOrderRequest direct = new DirectOrderRequest();
        direct.setVariantId(variant.getId());
        direct.setQuantity(1);
        direct.setAddressId(address.getId());
        direct.setPaymentMethod("COD");

        mockMvc.perform(post("/orders/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(direct)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeCodOrder_belowFreeThreshold_chargesRegionShipping() throws Exception {
        // 1 unit = 250000 < 500000 free-ship threshold; default address region SOUTH -> 35000
        AddCartItemRequest addReq = new AddCartItemRequest(variant.getId(), 1);
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.order.shippingCost").value(35000))
                .andExpect(jsonPath("$.data.order.grandTotal").value(285000))
                .andExpect(jsonPath("$.data.order.shippingRegion").value("SOUTH"));
    }

    @Test
    void shippingQuote_returnsRegionFeeAndThreshold() throws Exception {
        mockMvc.perform(get("/shipping/quote?region=NORTH&subtotal=100000")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee").value(25000))
                .andExpect(jsonPath("$.data.freeThreshold").value(500000));

        mockMvc.perform(get("/shipping/quote?region=NORTH&subtotal=500000")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee").value(0));
    }
}
