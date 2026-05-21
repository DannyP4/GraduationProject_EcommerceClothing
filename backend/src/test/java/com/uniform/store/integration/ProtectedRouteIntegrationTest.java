package com.uniform.store.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProtectedRouteIntegrationTest extends BaseIntegrationTest {

    @Test
    void getCart_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCart_withInvalidJwt_returns401() throws Exception {
        mockMvc.perform(get("/cart").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
