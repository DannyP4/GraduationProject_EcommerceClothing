package com.uniform.store.integration;

import com.uniform.store.entity.User;
import com.uniform.store.enums.AuthProvider;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.security.OAuthUserInfo;
import com.uniform.store.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuthIntegrationTest extends BaseIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @Test
    void newGoogleUser_createdAsCustomer_withNullPassword_thenExchangeReturnsJwt() throws Exception {
        String code = authService.startOAuthHandoff(
                new OAuthUserInfo("newg@gmail.com", true, "google-sub-1", "New Google"));

        User created = userRepository.findByEmail("newg@gmail.com").orElseThrow();
        assertThat(created.getRole().getName()).isEqualTo("customer");
        assertThat(created.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(created.getPasswordHash()).isNull();
        assertThat(created.getEmailVerifiedAt()).isNotNull();
        assertThat(created.getOauthSubject()).isEqualTo("google-sub-1");

        mockMvc.perform(post("/auth/oauth/exchange").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("newg@gmail.com"))
                .andExpect(jsonPath("$.data.user.role").value("customer"));
    }

    @Test
    void existingLocalUser_verifiedEmail_isLinked_keepsPassword() {
        data.createCustomer("local@uniform.test", "Pass1234");

        authService.startOAuthHandoff(new OAuthUserInfo("local@uniform.test", true, "google-sub-2", "Local U"));

        User linked = userRepository.findByEmail("local@uniform.test").orElseThrow();
        assertThat(linked.getOauthSubject()).isEqualTo("google-sub-2");
        assertThat(linked.getPasswordHash()).isNotNull();
        assertThat(linked.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
    }

    @Test
    void existingLocalUser_unverifiedEmail_linkRejected() {
        data.createCustomer("local2@uniform.test", "Pass1234");

        assertThatThrownBy(() -> authService.startOAuthHandoff(
                new OAuthUserInfo("local2@uniform.test", false, "google-sub-3", "Local U")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not verified");
    }

    @Test
    void returningGoogleUser_matchedBySubject_evenIfEmailChanged_noDuplicate() {
        authService.startOAuthHandoff(new OAuthUserInfo("first@gmail.com", true, "google-sub-4", "First"));
        long before = userRepository.count();

        authService.startOAuthHandoff(new OAuthUserInfo("changed@gmail.com", true, "google-sub-4", "First"));

        assertThat(userRepository.count()).isEqualTo(before);
        assertThat(userRepository.findByOauthSubject("google-sub-4").orElseThrow().getEmail())
                .isEqualTo("first@gmail.com");
    }

    @Test
    void googleUser_cannotFormLogin_getsGuidance() throws Exception {
        authService.startOAuthHandoff(new OAuthUserInfo("noform@gmail.com", true, "google-sub-5", "No Form"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"noform@gmail.com\",\"password\":\"whatever123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Google sign-in")));
    }

    @Test
    void exchange_bogusCode_returns400() throws Exception {
        mockMvc.perform(post("/auth/oauth/exchange").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "bogus-code"))))
                .andExpect(status().isBadRequest());
    }
}
