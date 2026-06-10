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

    public record FunctionCall(String name, JsonNode args, String id) {
    }

    public record FunctionDecl(String name, String description, Map<String, Object> parameters) {
    }

    // One conversation turn as raw Gemini content
    public record Content(Map<String, Object> map) {
        public static Content text(String role, String text) {
            return new Content(Map.of("role", role, "parts", List.of(Map.of("text", text))));
        }

        public static Content functionCall(FunctionCall call) {
            Map<String, Object> fc = new HashMap<>();
            fc.put("name", call.name());
            fc.put("args", call.args() == null ? Map.of() : call.args());
            if (call.id() != null) fc.put("id", call.id());
            return new Content(Map.of("role", "model", "parts", List.of(Map.of("functionCall", fc))));
        }

        public static Content functionResponse(String name, Map<String, Object> response, String id) {
            Map<String, Object> fr = new HashMap<>();
            fr.put("name", name);
            fr.put("response", response);
            if (id != null) fr.put("id", id);
            return new Content(Map.of("role", "user", "parts", List.of(Map.of("functionResponse", fr))));
        }

        public String role() {
            return (String) map.get("role");
        }

        public String text() {
            Object parts = map.get("parts");
            if (parts instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                Object t = first.get("text");
                return t == null ? null : t.toString();
            }
            return null;
        }
    }

    // Outcome of one generation: a function call to execute, or final text.
    public record Reply(String text, FunctionCall functionCall) {
        public boolean isCall() {
            return functionCall != null;
        }
    }

    public String generate(String systemInstruction, List<Msg> contents) {
        List<Content> mapped = contents.stream().map(m -> Content.text(m.role(), m.text())).toList();
        Reply reply = generateWithTools(systemInstruction, mapped, List.of());
        return reply == null ? null : reply.text();
    }

    public Reply generateWithTools(String systemInstruction, List<Content> contents, List<FunctionDecl> tools) {
        URI uri = URI.create(props.getBaseUrl() + "/models/" + props.getChatModel() + ":generateContent");

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents.stream().map(Content::map).toList());
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        }
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> decls = tools.stream().map(t -> Map.of(
                    "name", (Object) t.name(),
                    "description", t.description(),
                    "parameters", t.parameters())).toList();
            body.put("tools", List.of(Map.of("functionDeclarations", decls)));
            body.put("toolConfig", Map.of("functionCallingConfig", Map.of("mode", "AUTO")));
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
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            JsonNode fc = part.get("functionCall");
            if (fc != null && fc.has("name")) {
                String id = fc.hasNonNull("id") ? fc.path("id").asText() : null;
                return new Reply(null, new FunctionCall(fc.path("name").asText(), fc.get("args"), id));
            }
            text.append(part.path("text").asText(""));
        }
        return new Reply(text.toString(), null);
    }
}
