package com.uniform.store.integration;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderEmailType;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.event.OrderEmailEvent;
import com.uniform.store.repository.EmailLogRepository;
import com.uniform.store.service.AdminOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RecordApplicationEvents
class OrderEmailIntegrationTest extends BaseIntegrationTest {

    @Autowired ApplicationEvents applicationEvents;
    @Autowired AdminOrderService adminOrderService;
    @Autowired EmailLogRepository emailLogRepository;

    User buyer;
    User admin;
    String jwt;
    Address address;
    ProductVariant variant;

    @BeforeEach
    void seed() {
        buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        admin = data.createAdmin("admin@uniform.test", "Test1234");
        jwt = data.accessTokenFor(buyer.getEmail());
        address = data.createDefaultAddress(buyer);
        Brand brand = data.createBrand();
        Category category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        variant = data.createVariant(product, 20);
    }

    @Test
    void placingCodOrder_publishesConfirmationEvent_andSendsNoMailWhenDisabled() throws Exception {
        AddCartItemRequest addReq = new AddCartItemRequest(variant.getId(), 1);
        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");
        mockMvc.perform(post("/orders").header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated());

        List<OrderEmailType> published = applicationEvents.stream(OrderEmailEvent.class)
                .map(OrderEmailEvent::type).toList();
        assertThat(published).contains(OrderEmailType.CONFIRMATION);

        assertThat(emailLogRepository.count()).as("mail disabled in tests → no real send").isZero();
    }

    @Test
    void adminTransitionToShipped_publishesShippedEvent() {
        Order seeded = data.createOrderWithItem(buyer, variant, 1,
                OrderStatus.PROCESSING, Instant.now(), PaymentProvider.COD);

        adminOrderService.transitionOrder(seeded.getOrderNumber(),
                OrderStatus.SHIPPED, null, admin.getEmail());

        List<OrderEmailType> published = applicationEvents.stream(OrderEmailEvent.class)
                .map(OrderEmailEvent::type).toList();
        assertThat(published).contains(OrderEmailType.SHIPPED);
    }
}
