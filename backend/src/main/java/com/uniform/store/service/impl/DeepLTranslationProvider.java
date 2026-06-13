package com.uniform.store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniform.store.config.DeepLProperties;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.TranslationQuotaException;
import com.uniform.store.service.TranslationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeepLTranslationProvider implements TranslationProvider {

    private final RestClient deeplRestClient;
    private final DeepLProperties props;

    @Override
    public boolean isEnabled() {
        return props.isEnabled() && props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    @Override
    public List<String> translate(List<String> texts, String targetLocale) {
        if (texts.isEmpty()) return List.of();
        if (!isEnabled()) throw new BadRequestException("DeepL translation is not configured");

        Map<String, Object> body = Map.of(
                "text", texts,
                "source_lang", "EN",
                "target_lang", deeplTarget(targetLocale));

        RestClientException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                JsonNode resp = deeplRestClient.post()
                        .uri("/v2/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                if (resp == null || !resp.has("translations")) {
                    throw new BadRequestException("DeepL returned an unexpected response");
                }
                List<String> out = new ArrayList<>(texts.size());
                for (JsonNode t : resp.get("translations")) out.add(t.path("text").asText(""));
                return out;
            } catch (RestClientResponseException e) {
                last = e;
                int status = e.getStatusCode().value();
                if (status == 456) {
                    throw new TranslationQuotaException("DeepL monthly character limit reached (456)");
                }
                if (status != 429 && status < 500) {
                    throw new BadRequestException("DeepL API call failed (" + status + "): " + e.getResponseBodyAsString());
                }
                if (attempt < 3) sleep(attempt * 1000L);
            } catch (RestClientException e) {
                last = e;
                if (attempt < 3) sleep(attempt * 1000L);
            }
        }
        throw new BadRequestException("DeepL API call failed after retries: "
                + (last != null ? last.getMessage() : "unknown"));
    }

    private static String deeplTarget(String locale) {
        return switch (locale.toLowerCase(Locale.ROOT)) {
            case "vi" -> "VI";
            case "ja" -> "JA";
            default -> throw new BadRequestException("Unsupported target locale: " + locale);
        };
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
