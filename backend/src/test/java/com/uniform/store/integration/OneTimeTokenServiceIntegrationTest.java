package com.uniform.store.integration;

import com.uniform.store.entity.OneTimeToken;
import com.uniform.store.enums.TokenType;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.service.OneTimeTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OneTimeTokenServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired OneTimeTokenService tokenService;

    @Test
    void issueThenConsume_marksConsumed() {
        String raw = tokenService.issue(TokenType.PASSWORD_RESET, "a@test.com", null, Duration.ofMinutes(30), null);

        OneTimeToken consumed = tokenService.consume(raw, TokenType.PASSWORD_RESET);

        assertThat(consumed.getConsumedAt()).isNotNull();
        assertThat(consumed.getEmail()).isEqualTo("a@test.com");
    }

    @Test
    void consumeTwice_throwsAlreadyUsed() {
        String raw = tokenService.issue(TokenType.PASSWORD_RESET, "a@test.com", null, Duration.ofMinutes(30), null);
        tokenService.consume(raw, TokenType.PASSWORD_RESET);

        assertThatThrownBy(() -> tokenService.consume(raw, TokenType.PASSWORD_RESET))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void consumeWrongType_throwsInvalid() {
        String raw = tokenService.issue(TokenType.PASSWORD_RESET, "a@test.com", null, Duration.ofMinutes(30), null);

        assertThatThrownBy(() -> tokenService.consume(raw, TokenType.EMAIL_VERIFY))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    void consumeExpired_throwsExpired() {
        String raw = tokenService.issue(TokenType.EMAIL_VERIFY, "a@test.com", null, Duration.ofMinutes(-1), null);

        assertThatThrownBy(() -> tokenService.consume(raw, TokenType.EMAIL_VERIFY))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void consumeUnknownToken_throwsInvalid() {
        assertThatThrownBy(() -> tokenService.consume("nonexistent", TokenType.PASSWORD_RESET))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    void peek_doesNotConsume_consumeStillWorksAfter() {
        String raw = tokenService.issue(TokenType.ADMIN_INVITE, "a@test.com", null, Duration.ofHours(1), null);

        OneTimeToken peeked = tokenService.peek(raw, TokenType.ADMIN_INVITE);
        assertThat(peeked.getConsumedAt()).isNull();
        assertThat(peeked.getEmail()).isEqualTo("a@test.com");

        OneTimeToken consumed = tokenService.consume(raw, TokenType.ADMIN_INVITE);
        assertThat(consumed.getConsumedAt()).isNotNull();
    }

    @Test
    void peek_consumedToken_throwsAlreadyUsed() {
        String raw = tokenService.issue(TokenType.ADMIN_INVITE, "a@test.com", null, Duration.ofHours(1), null);
        tokenService.consume(raw, TokenType.ADMIN_INVITE);

        assertThatThrownBy(() -> tokenService.peek(raw, TokenType.ADMIN_INVITE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been used");
    }
}
