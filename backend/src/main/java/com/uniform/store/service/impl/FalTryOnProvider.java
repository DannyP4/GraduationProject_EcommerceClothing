package com.uniform.store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniform.store.config.FalProperties;
import com.uniform.store.enums.TryOnStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.TryOnException;
import com.uniform.store.service.VirtualTryOnProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FalTryOnProvider implements VirtualTryOnProvider {

    private static final String PROVIDER_NAME = "FAL_FASHN";

    private final RestClient falRestClient;
    private final FalProperties props;

    @Override
    public boolean isEnabled() {
        return props.isEnabled() && props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public Submission submit(String modelImageUrl, String garmentImageUrl, String garmentPhotoType, String category) {
        if (!isEnabled()) throw new TryOnException("Virtual try-on provider is not configured");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model_image", modelImageUrl);
        body.put("garment_image", garmentImageUrl);
        body.put("garment_photo_type", garmentPhotoType);
        if (category != null && !category.isBlank()) {
            body.put("category", category);
        }

        RestClientException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                JsonNode resp = falRestClient.post()
                        .uri("/" + props.getModel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                if (resp == null || !resp.hasNonNull("request_id") || !resp.hasNonNull("response_url")) {
                    throw new TryOnException("fal.ai returned an unexpected submit response");
                }
                return new Submission(resp.get("request_id").asText(), resp.get("response_url").asText());
            } catch (RestClientResponseException e) {
                last = e;
                int status = e.getStatusCode().value();
                if (status != 429 && status < 500) {
                    throw new BadRequestException("Try-on request rejected (" + status + "): " + e.getResponseBodyAsString());
                }
                if (attempt < 3) sleep(attempt * 1000L);
            } catch (RestClientException e) {
                last = e;
                if (attempt < 3) sleep(attempt * 1000L);
            }
        }
        throw new TryOnException("fal.ai submit failed after retries: " + (last != null ? last.getMessage() : "unknown"));
    }

    @Override
    public PollResult poll(String responseUrl) {
        if (responseUrl == null || responseUrl.isBlank()) {
            return new PollResult(TryOnStatus.FAILED, null, "Missing response url");
        }

        String statusText;
        try {
            JsonNode status = falRestClient.get()
                    .uri(responseUrl + "/status")
                    .retrieve()
                    .body(JsonNode.class);
            statusText = status == null ? "" : status.path("status").asText("");
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code != 429 && code < 500) {
                return new PollResult(TryOnStatus.FAILED, null, "Status check rejected (" + code + ")");
            }
            throw new TryOnException("fal.ai status check failed (" + code + ")");
        } catch (RestClientException e) {
            throw new TryOnException("fal.ai status check failed: " + e.getMessage());
        }

        if (!"COMPLETED".equalsIgnoreCase(statusText)) {
            return new PollResult(TryOnStatus.PROCESSING, null, null);
        }

        try {
            JsonNode result = falRestClient.get()
                    .uri(responseUrl)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode images = result == null ? null : result.path("images");
            if (images == null || !images.isArray() || images.isEmpty()) {
                return new PollResult(TryOnStatus.FAILED, null, "fal.ai returned no images");
            }
            String url = images.get(0).path("url").asText("");
            if (url.isBlank()) {
                return new PollResult(TryOnStatus.FAILED, null, "fal.ai result missing image url");
            }
            return new PollResult(TryOnStatus.SUCCEEDED, url, null);
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code != 429 && code < 500) {
                return new PollResult(TryOnStatus.FAILED, null, "Result fetch rejected (" + code + ")");
            }
            throw new TryOnException("fal.ai result fetch failed (" + code + ")");
        } catch (RestClientException e) {
            throw new TryOnException("fal.ai result fetch failed: " + e.getMessage());
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
