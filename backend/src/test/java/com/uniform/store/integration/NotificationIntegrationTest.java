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
import com.uniform.store.repository.NotificationRepository;
import com.uniform.store.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RecordApplicationEvents
class NotificationIntegrationTest extends BaseIntegrationTest {

    @Autowired ApplicationEvents applicationEvents;
    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;

    User buyer;
    User other;
    String buyerJwt;
    String otherJwt;
    Address address;
    ProductVariant variant;

    @BeforeEach
    void seed() {
        buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        other = data.createCustomer("other@uniform.test", "Test1234");
        buyerJwt = data.accessTokenFor(buyer.getEmail());
        otherJwt = data.accessTokenFor(other.getEmail());
        address = data.createDefaultAddress(buyer);
        Brand brand = data.createBrand();
        Category category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        variant = data.createVariant(product, 20);
    }

    private Order seedOrder() {
        return data.createOrderWithItem(buyer, variant, 1,
                OrderStatus.PROCESSING, Instant.now(), PaymentProvider.COD);
    }

    @Test
    void placingCodOrder_publishesOrderEvent_thatDrivesNotifications() throws Exception {
        AddCartItemRequest addReq = new AddCartItemRequest(variant.getId(), 1);
        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        PlaceOrderRequest order = new PlaceOrderRequest();
        order.setAddressId(address.getId());
        order.setPaymentMethod("COD");
        mockMvc.perform(post("/orders").header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated());

        List<OrderEmailType> published = applicationEvents.stream(OrderEmailEvent.class)
                .map(OrderEmailEvent::type).toList();
        assertThat(published).contains(OrderEmailType.CONFIRMATION);
    }

    @Test
    void createForOrderEvent_persistsNotification_visibleViaApi() throws Exception {
        Order order = seedOrder();

        notificationService.createForOrderEvent(order.getId(), OrderEmailType.SHIPPED);

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("ORDER_SHIPPED"))
                .andExpect(jsonPath("$.data.content[0].read").value(false))
                .andExpect(jsonPath("$.data.content[0].message").value(
                        "Order #" + order.getOrderNumber() + " is on its way."))
                .andExpect(jsonPath("$.data.content[0].href").value(
                        "/account/orders/" + order.getOrderNumber()));
    }

    @Test
    void markRead_decrementsUnreadCount() throws Exception {
        Order order = seedOrder();
        notificationService.createForOrderEvent(order.getId(), OrderEmailType.SHIPPED);
        notificationService.createForOrderEvent(order.getId(), OrderEmailType.DELIVERED);

        mockMvc.perform(get("/notifications/unread-count").header("Authorization", "Bearer " + buyerJwt))
                .andExpect(jsonPath("$.data").value(2));

        Long id = notificationRepository.findByUserIdOrderByCreatedAtDesc(buyer.getId(), PageRequest.of(0, 10))
                .getContent().get(0).getId();
        mockMvc.perform(post("/notifications/" + id + "/read").header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/notifications/unread-count").header("Authorization", "Bearer " + buyerJwt))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void markAllRead_clearsUnreadCount() throws Exception {
        Order order = seedOrder();
        notificationService.createForOrderEvent(order.getId(), OrderEmailType.SHIPPED);
        notificationService.createForOrderEvent(order.getId(), OrderEmailType.DELIVERED);

        mockMvc.perform(post("/notifications/read-all").header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        mockMvc.perform(get("/notifications/unread-count").header("Authorization", "Bearer " + buyerJwt))
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void notificationsAreScopedToOwner() throws Exception {
        Order order = seedOrder();
        notificationService.createForOrderEvent(order.getId(), OrderEmailType.SHIPPED);
        Long buyerNotifId = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(buyer.getId(), PageRequest.of(0, 10))
                .getContent().get(0).getId();

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(post("/notifications/" + buyerNotifId + "/read")
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void purgeExpired_removesNotificationsOlderThanRetention() {
        Order order = seedOrder();
        notificationService.createForOrderEvent(order.getId(), OrderEmailType.SHIPPED);
        notificationService.createForOrderEvent(order.getId(), OrderEmailType.DELIVERED);

        Long oldId = notificationRepository.findByUserIdOrderByCreatedAtDesc(buyer.getId(), PageRequest.of(0, 10))
                .getContent().get(0).getId();
        jdbc.update("UPDATE notifications SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(200, ChronoUnit.DAYS)), oldId);

        int deleted = notificationService.purgeExpired();

        assertThat(deleted).isEqualTo(1);
        assertThat(notificationRepository.findById(oldId)).isEmpty();
        assertThat(notificationRepository.count()).isEqualTo(1);
    }
}
