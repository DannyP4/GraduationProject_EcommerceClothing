package com.uniform.store.integration;

import com.uniform.store.dto.request.LoginRequest;
import com.uniform.store.entity.User;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUserIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;

    private User admin;
    private User customer;
    private String adminJwt;
    private String customerJwt;

    @BeforeEach
    void seed() {
        admin = data.createAdmin("admin@uniform.test", "Admin1234");
        adminJwt = data.accessTokenFor(admin.getEmail());
        customer = data.createCustomer("buyer@uniform.test", "Pass1234");
        customerJwt = data.accessTokenFor(customer.getEmail());
    }

    @Test
    void list_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_withAdminJwt_returnsPagedUsers() throws Exception {
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void list_filterByStatusSuspended_returnsOnlySuspended() throws Exception {
        customer.setStatus(UserStatus.SUSPENDED);
        userRepository.save(customer);

        mockMvc.perform(get("/admin/users")
                        .param("status", "SUSPENDED")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("buyer@uniform.test"))
                .andExpect(jsonPath("$.data.content[0].status").value("SUSPENDED"));
    }

    @Test
    void list_filterBySearch_matchesEmailOrName() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("search", "buyer")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("buyer@uniform.test"));
    }

    @Test
    void get_returnsUserDetailWithOrders() throws Exception {
        mockMvc.perform(get("/admin/users/" + customer.getId()).header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(customer.getId()))
                .andExpect(jsonPath("$.data.email").value("buyer@uniform.test"))
                .andExpect(jsonPath("$.data.roleName").value("customer"))
                .andExpect(jsonPath("$.data.ordersCount").value(0));
    }

    @Test
    void get_missingUser_returns404() throws Exception {
        mockMvc.perform(get("/admin/users/99999").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void suspend_customer_succeeds() throws Exception {
        mockMvc.perform(post("/admin/users/" + customer.getId() + "/suspend")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        assertThat(userRepository.findById(customer.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void suspend_admin_returns400() throws Exception {
        User otherAdmin = data.createAdmin("admin2@uniform.test", "Admin1234");
        mockMvc.perform(post("/admin/users/" + otherAdmin.getId() + "/suspend")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("admin")));
    }

    @Test
    void suspend_self_returns400() throws Exception {
        mockMvc.perform(post("/admin/users/" + admin.getId() + "/suspend")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activate_suspended_succeeds() throws Exception {
        customer.setStatus(UserStatus.SUSPENDED);
        userRepository.save(customer);

        mockMvc.perform(post("/admin/users/" + customer.getId() + "/activate")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void softDelete_customer_succeeds() throws Exception {
        mockMvc.perform(delete("/admin/users/" + customer.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        assertThat(userRepository.findById(customer.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.DELETED);
    }

    @Test
    void softDelete_admin_returns400() throws Exception {
        User otherAdmin = data.createAdmin("admin3@uniform.test", "Admin1234");
        mockMvc.perform(delete("/admin/users/" + otherAdmin.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void softDelete_self_returns400() throws Exception {
        mockMvc.perform(delete("/admin/users/" + admin.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_suspendedUser_returns401() throws Exception {
        customer.setStatus(UserStatus.SUSPENDED);
        userRepository.save(customer);

        LoginRequest req = new LoginRequest();
        req.setEmail("buyer@uniform.test");
        req.setPassword("Pass1234");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("suspended")));
    }

    @Test
    void login_deletedUser_returns401() throws Exception {
        customer.setStatus(UserStatus.DELETED);
        userRepository.save(customer);

        LoginRequest req = new LoginRequest();
        req.setEmail("buyer@uniform.test");
        req.setPassword("Pass1234");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("deleted")));
    }

    @Test
    void existingJwt_revokedWhenStatusFlippedToSuspended() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk());

        customer.setStatus(UserStatus.SUSPENDED);
        userRepository.save(customer);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isUnauthorized());
    }
}
