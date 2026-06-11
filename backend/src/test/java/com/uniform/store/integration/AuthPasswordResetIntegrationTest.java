package com.uniform.store.integration;

import com.uniform.store.entity.User;
import com.uniform.store.enums.TokenType;
import com.uniform.store.event.AuthMailEvent;
import com.uniform.store.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RecordApplicationEvents
class AuthPasswordResetIntegrationTest extends BaseIntegrationTest {

    @Autowired ApplicationEvents applicationEvents;
    @Autowired UserRepository userRepository;

    private String tokenFromEvent(TokenType type) {
        AuthMailEvent event = applicationEvents.stream(AuthMailEvent.class)
                .filter(e -> e.type() == type)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No " + type + " mail event published"));
        String link = event.link();
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    @Test
    void forgotThenReset_allowsLoginWithNewPasswordOnly() throws Exception {
        data.createCustomer("reset@uniform.test", "OldPass123");

        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset@uniform.test\"}"))
                .andExpect(status().isOk());

        String token = tokenFromEvent(TokenType.PASSWORD_RESET);
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token, "newPassword", "NewPass456"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset@uniform.test\",\"password\":\"NewPass456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset@uniform.test\",\"password\":\"OldPass123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_thenVerifyEmail_setsVerifiedAt() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"verify@uniform.test\",\"password\":\"Pass1234\",\"fullName\":\"Ver\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.emailVerified").value(false));

        String token = tokenFromEvent(TokenType.EMAIL_VERIFY);
        mockMvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("verify@uniform.test").orElseThrow();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    void forgotPassword_unknownEmail_returns200_andPublishesNoEvent() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@uniform.test\"}"))
                .andExpect(status().isOk());

        assertThat(applicationEvents.stream(AuthMailEvent.class)
                .filter(e -> e.type() == TokenType.PASSWORD_RESET).count()).isZero();
    }

    @Test
    void resetPassword_invalidToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "bogus", "newPassword", "NewPass456"))))
                .andExpect(status().isBadRequest());
    }
}
