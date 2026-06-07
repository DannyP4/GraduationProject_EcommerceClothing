package com.uniform.store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniform.store.config.GeminiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiChatClient {

    private final GeminiProperties props;
    private final RestClient geminiRestClient;

    public GeminiChatClient(GeminiProperties props, RestClient geminiRestClient) {
        this.props = props;
        this.geminiRestClient = geminiRestClient;
    }

    public record Msg(String role, String text) {
    }

    public String generate(String systemInstruction, List<Msg> contents) {
        URI uri = URI.create(props.getBaseUrl() + "/models/" + props.getChatModel() + ":generateContent");

        List<Map<String, Object>> contentList = contents.stream()
                .map(m -> Map.<String, Object>of(
                        "role", m.role(),
                        "parts", List.of(Map.of("text", m.text()))))
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contentList);
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        }
        body.put("generationConfig", Map.of(
                "temperature", props.getTemperature(),
                "maxOutputTokens", props.getMaxOutputTokens(),
                "thinkingConfig", Map.of("thinkingBudget", props.getThinkingBudget())));

        JsonNode resp = GeminiHttp.post(geminiRestClient, uri, body);
        JsonNode candidates = resp.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            sb.append(part.path("text").asText(""));
        }
        return sb.toString();
    }
}
