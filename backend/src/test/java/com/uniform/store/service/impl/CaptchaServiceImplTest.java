package com.uniform.store.service.impl;

import com.uniform.store.config.AppCaptchaProperties;
import com.uniform.store.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CaptchaServiceImplTest {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    AppCaptchaProperties props;
    MockRestServiceServer server;
    CaptchaServiceImpl service;

    @BeforeEach
    void setUp() {
        props = new AppCaptchaProperties();
        props.setEnabled(true);
        props.getTurnstile().setSecretKey("secret");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new CaptchaServiceImpl(props, builder.build());
    }

    @Test
    void disabled_skipsHttpAndPasses() {
        props.setEnabled(false);
        assertThatCode(() -> service.verify(null, null)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void enabled_blankToken_throws() {
        assertThatThrownBy(() -> service.verify("  ", "1.2.3.4"))
                .isInstanceOf(BadRequestException.class);
        server.verify();
    }

    @Test
    void enabled_success_passes() {
        server.expect(requestTo(VERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> service.verify("good-token", "1.2.3.4")).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void enabled_failure_throws() {
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.verify("bad-token", "1.2.3.4"))
                .isInstanceOf(BadRequestException.class);
        server.verify();
    }
}
