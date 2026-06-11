package com.uniform.store.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniform.store.entity.User;
import com.uniform.store.enums.TokenType;
import com.uniform.store.event.AuthMailEvent;
import com.uniform.store.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RecordApplicationEvents
class AdminInviteIntegrationTest extends BaseIntegrationTest {

    @Autowired ApplicationEvents applicationEvents;
    @Autowired UserRepository userRepository;

    private String adminJwt;
    private String customerJwt;

    @BeforeEach
    void seed() {
        adminJwt = data.accessTokenFor(data.createAdmin("admin@uniform.test", "Admin1234").getEmail());
        customerJwt = data.accessTokenFor(data.createCustomer("buyer@uniform.test", "Pass1234").getEmail());
    }

    private String inviteTokenFromEvent() {
        AuthMailEvent event = applicationEvents.stream(AuthMailEvent.class)
                .filter(e -> e.type() == TokenType.ADMIN_INVITE)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No ADMIN_INVITE mail event published"));
        String link = event.link();
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    @Test
    void invite_withAdminJwt_publishesInviteEvent() throws Exception {
        mockMvc.perform(post("/admin/users/invite")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newadmin@uniform.test\",\"fullName\":\"New Admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("newadmin@uniform.test"));

        assertThat(applicationEvents.stream(AuthMailEvent.class)
                .filter(e -> e.type() == TokenType.ADMIN_INVITE)
                .anyMatch(e -> e.recipient().equals("newadmin@uniform.test"))).isTrue();
    }

    @Test
    void invite_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(post("/admin/users/invite")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newadmin@uniform.test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invite_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/admin/users/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newadmin@uniform.test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invite_existingEmail_returns400() throws Exception {
        mockMvc.perform(post("/admin/users/invite")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"buyer@uniform.test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewInvite_returnsInvitedEmailAndName() throws Exception {
        mockMvc.perform(post("/admin/users/invite")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newadmin@uniform.test\",\"fullName\":\"New Admin\"}"))
                .andExpect(status().isOk());

        String token = inviteTokenFromEvent();
        mockMvc.perform(get("/auth/invite").param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("newadmin@uniform.test"))
                .andExpect(jsonPath("$.data.fullName").value("New Admin"));
    }

    @Test
    void acceptInvite_createsAdmin_thatCanAccessAdminApi() throws Exception {
        mockMvc.perform(post("/admin/users/invite")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newadmin@uniform.test\"}"))
                .andExpect(status().isOk());

        String token = inviteTokenFromEvent();
        MvcResult result = mockMvc.perform(post("/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token, "fullName", "Brand New", "password", "Admin5678"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("admin"))
                .andExpect(jsonPath("$.data.user.emailVerified").value(true))
                .andReturn();

        User created = userRepository.findByEmail("newadmin@uniform.test").orElseThrow();
        assertThat(created.getEmailVerifiedAt()).isNotNull();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String newAdminToken = body.path("data").path("accessToken").asText();
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + newAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newadmin@uniform.test\",\"password\":\"Admin5678\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void acceptInvite_twice_secondReturns400() throws Exception {
        mockMvc.perform(post("/admin/users/invite")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newadmin@uniform.test\"}"))
                .andExpect(status().isOk());

        String token = inviteTokenFromEvent();
        String acceptBody = objectMapper.writeValueAsString(
                Map.of("token", token, "fullName", "Brand New", "password", "Admin5678"));

        mockMvc.perform(post("/auth/accept-invite").contentType(MediaType.APPLICATION_JSON).content(acceptBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/accept-invite").contentType(MediaType.APPLICATION_JSON).content(acceptBody))
                .andExpect(status().isBadRequest());
    }
}
