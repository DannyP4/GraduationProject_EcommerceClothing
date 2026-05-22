package com.uniform.store.integration;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.request.TransitionOrderRequest;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOrderIntegrationTest extends BaseIntegrationTest {

    @Autowired ProductVariantRepository variantRepository;
    @Autowired OrderRepository orderRepository;

    User customer;
    User adminUser;
    String customerJwt;
    String adminJwt;
    Address address;
    ProductVariant variant;
    String firstOrderNumber;

    @BeforeEach
    void seed() throws Exception {
        customer = data.createCustomer("buyer@uniform.test", "Test1234");
        customerJwt = data.accessTokenFor(customer.getEmail());

        adminUser = data.createAdmin("admin@uniform.test", "Admin1234");
        adminJwt = data.accessTokenFor(adminUser.getEmail());

        address = data.createDefaultAddress(customer);
        Brand brand = data.createBrand();
        Category category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        variant = data.createVariant(product, 20);

        firstOrderNumber = placeCodOrder(2);
    }

    @Test
    void list_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(get("/admin/orders")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withAdminJwt_returnsOrdersIncludingCustomerInfo() throws Exception {
        mockMvc.perform(get("/admin/orders")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderNumber").value(firstOrderNumber))
                .andExpect(jsonPath("$.data.content[0].customer.email").value("buyer@uniform.test"))
                .andExpect(jsonPath("$.data.content[0].customer.fullName").exists())
                .andExpect(jsonPath("$.data.content[0].paymentProvider").value("COD"));
    }

    @Test
    void list_filterByStatus_returnsOnlyMatchingOrders() throws Exception {
        mockMvc.perform(get("/admin/orders?status=PENDING")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/admin/orders?status=DELIVERED")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getDetail_byOrderNumber_returnsCustomerAndAllowedTransitions() throws Exception {
        mockMvc.perform(get("/admin/orders/" + firstOrderNumber)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.orderNumber").value(firstOrderNumber))
                .andExpect(jsonPath("$.data.customer.email").value("buyer@uniform.test"))
                .andExpect(jsonPath("$.data.cancellableByAdmin").value(true))
                .andExpect(jsonPath("$.data.requiresRefund").value(false));
    }

    @Test
    void transition_invalidTarget_returns400() throws Exception {
        TransitionOrderRequest req = new TransitionOrderRequest();
        req.setTargetStatus(OrderStatus.DELIVERED);

        mockMvc.perform(patch("/admin/orders/" + firstOrderNumber + "/transition")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid transition")));
    }

    @Test
    void transition_validNext_updatesStatusAndHistory() throws Exception {
        Order paid = orderRepository.findByOrderNumber(firstOrderNumber).orElseThrow();
        paid.setStatus(OrderStatus.PAID);
        orderRepository.save(paid);

        TransitionOrderRequest req = new TransitionOrderRequest();
        req.setTargetStatus(OrderStatus.PROCESSING);
        req.setNote("Packed by warehouse-A");

        String resp = mockMvc.perform(patch("/admin/orders/" + firstOrderNumber + "/transition")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value("PROCESSING"))
                .andReturn().getResponse().getContentAsString();

        assertThat(resp).contains("admin@uniform.test").contains("Packed by warehouse-A");

        assertThat(orderRepository.findByOrderNumber(firstOrderNumber).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void cancel_pendingOrder_restoresStockAndSetsCancelled() throws Exception {
        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity())
                .as("after order placement 20 - 2 = 18").isEqualTo(18);

        mockMvc.perform(post("/admin/orders/" + firstOrderNumber + "/cancel")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Customer phone request\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value("CANCELLED"));

        assertThat(variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity())
                .as("stock restored to 20 after admin cancel").isEqualTo(20);
    }

    @Test
    void cancel_shippedOrder_returns400() throws Exception {
        Order shipped = orderRepository.findByOrderNumber(firstOrderNumber).orElseThrow();
        shipped.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(shipped);

        mockMvc.perform(post("/admin/orders/" + firstOrderNumber + "/cancel")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Cannot cancel")));
    }

    private String placeCodOrder(int qty) throws Exception {
        AddCartItemRequest addReq = new AddCartItemRequest(variant.getId(), qty);
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");

        String resp = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resp).path("data").path("order").path("orderNumber").asText();
    }
}
