package com.uniform.store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniform.store.exception.BadRequestException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;

final class GeminiHttp {

    private GeminiHttp() {
    }

    static JsonNode post(RestClient client, URI uri, Object body) {
        RestClientException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                JsonNode resp = client.post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                if (resp == null) throw new BadRequestException("Gemini returned an empty response body");
                return resp;
            } catch (RestClientResponseException e) {
                last = e;
                int status = e.getStatusCode().value();
                if (status != 429 && status < 500) {
                    throw new BadRequestException("Gemini API call failed (" + status + "): "
                            + e.getResponseBodyAsString());
                }
                if (attempt < 3) sleep(attempt * 1000L);
            } catch (RestClientException e) {
                last = e;
                if (attempt < 3) sleep(attempt * 1000L);
            }
        }
        throw new BadRequestException("Gemini API call failed after retries: "
                + (last != null ? last.getMessage() : "unknown"));
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
