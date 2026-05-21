package com.uniform.store.integration;

import com.uniform.store.dto.request.LoginRequest;
import com.uniform.store.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_then_login_then_me_succeedsEndToEnd() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("e2e1@uniform.test");
        reg.setPassword("Test1234");
        reg.setFullName("E2E User");
        reg.setPreferredLocale("vi");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email").value("e2e1@uniform.test"));

        LoginRequest login = new LoginRequest();
        login.setEmail("e2e1@uniform.test");
        login.setPassword("Test1234");

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginBody).path("data").path("accessToken").asText();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("e2e1@uniform.test"))
                .andExpect(jsonPath("$.data.role").value("customer"));
    }

    @Test
    void register_duplicateEmail_rejectedWithBadRequest() throws Exception {
        data.createCustomer("dup@uniform.test", "Test1234");

        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("dup@uniform.test");
        reg.setPassword("Test1234");
        reg.setFullName("Dup");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_wrongPassword_returnsBadRequest() throws Exception {
        data.createCustomer("wrongpwd@uniform.test", "Test1234");

        LoginRequest login = new LoginRequest();
        login.setEmail("wrongpwd@uniform.test");
        login.setPassword("NotTheRightOne9");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }
}
