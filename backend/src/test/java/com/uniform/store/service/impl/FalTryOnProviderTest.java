package com.uniform.store.service.impl;

import com.uniform.store.config.FalProperties;
import com.uniform.store.enums.TryOnStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.service.VirtualTryOnProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FalTryOnProviderTest {

    private static final String RESPONSE_URL = "https://queue.test/m/v1/requests/abc";

    private MockRestServiceServer server;
    private FalTryOnProvider provider;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://queue.test");
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient rest = builder.build();
        FalProperties props = new FalProperties();
        props.setBaseUrl("https://queue.test");
        props.setModel("m/v1");
        props.setApiKey("test-key");
        provider = new FalTryOnProvider(rest, props);
    }

    @Test
    void submit_sendsImageFields_parsesRequestIdAndResponseUrl() {
        server.expect(requestTo("https://queue.test/m/v1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model_image").value("https://cdn/user.jpg"))
                .andExpect(jsonPath("$.garment_image").value("https://cdn/shirt.jpg"))
                .andExpect(jsonPath("$.garment_photo_type").value("auto"))
                .andExpect(jsonPath("$.category").value("tops"))
                .andRespond(withSuccess(
                        "{\"request_id\":\"abc\",\"response_url\":\"" + RESPONSE_URL + "\"}",
                        APPLICATION_JSON));

        VirtualTryOnProvider.Submission s =
                provider.submit("https://cdn/user.jpg", "https://cdn/shirt.jpg", "auto", "tops");

        assertThat(s.requestId()).isEqualTo("abc");
        assertThat(s.responseUrl()).isEqualTo(RESPONSE_URL);
        server.verify();
    }

    @Test
    void submit_clientError_throwsBadRequest() {
        server.expect(requestTo("https://queue.test/m/v1"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("bad image"));

        assertThatThrownBy(() -> provider.submit("u", "g", "auto", null))
                .isInstanceOf(BadRequestException.class);
        server.verify();
    }

    @Test
    void poll_inProgress_returnsProcessing() {
        server.expect(requestTo(RESPONSE_URL + "/status"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"IN_PROGRESS\"}", APPLICATION_JSON));

        VirtualTryOnProvider.PollResult r = provider.poll(RESPONSE_URL);

        assertThat(r.status()).isEqualTo(TryOnStatus.PROCESSING);
        assertThat(r.resultImageUrl()).isNull();
        server.verify();
    }

    @Test
    void poll_completed_fetchesResultImageUrl() {
        server.expect(requestTo(RESPONSE_URL + "/status"))
                .andRespond(withSuccess("{\"status\":\"COMPLETED\"}", APPLICATION_JSON));
        server.expect(requestTo(RESPONSE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"images\":[{\"url\":\"https://cdn/result.png\"}]}", APPLICATION_JSON));

        VirtualTryOnProvider.PollResult r = provider.poll(RESPONSE_URL);

        assertThat(r.status()).isEqualTo(TryOnStatus.SUCCEEDED);
        assertThat(r.resultImageUrl()).isEqualTo("https://cdn/result.png");
        server.verify();
    }

    @Test
    void poll_completedButNoImages_returnsFailed() {
        server.expect(requestTo(RESPONSE_URL + "/status"))
                .andRespond(withSuccess("{\"status\":\"COMPLETED\"}", APPLICATION_JSON));
        server.expect(requestTo(RESPONSE_URL))
                .andRespond(withSuccess("{\"images\":[]}", APPLICATION_JSON));

        VirtualTryOnProvider.PollResult r = provider.poll(RESPONSE_URL);

        assertThat(r.status()).isEqualTo(TryOnStatus.FAILED);
        server.verify();
    }
}
