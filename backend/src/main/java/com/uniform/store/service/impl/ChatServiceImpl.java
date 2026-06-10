package com.uniform.store.service.impl;

import com.uniform.store.config.GeminiProperties;
import com.uniform.store.config.ShippingProperties;
import com.uniform.store.dto.request.ChatRequest;
import com.uniform.store.dto.request.ChatTurn;
import com.uniform.store.dto.response.ChatResponse;
import com.uniform.store.dto.response.ProductSummaryDto;
import com.uniform.store.service.ChatService;
import com.uniform.store.service.EmbeddingService;
import com.uniform.store.service.ProductService;
import com.uniform.store.service.RetrievalService;
import com.uniform.store.service.RetrievalService.ScoredProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String FALLBACK_REPLY =
            "Sorry, I'm having trouble generating a response right now. " +
            "Please try asking again or browse our product categories for inspiration!";

    private static final String TOOL_SIMILAR = "find_similar_products";
    private static final String TOOL_FBT = "find_frequently_bought_together";
    private static final int MAX_TOOL_ROUNDS = 3;
    private static final int REC_LIMIT = 6;

    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final ProductService productService;
    private final GeminiChatClient chatClient;
    private final GeminiProperties props;
    private final ShippingProperties shippingProperties;

    @Override
    public ChatResponse chat(ChatRequest request, String locale) {
        String message = request.getMessage().trim();

        float[] queryVector = embeddingService.embedQuery(message);
        List<ScoredProduct> hits = retrievalService.search(queryVector, props.getRetrievalTopK());
        List<Long> relevantIds = hits.stream()
                .filter(h -> h.score() >= props.getScoreThreshold())
                .map(ScoredProduct::productId)
                .toList();

        boolean hasMatch = !relevantIds.isEmpty();
        List<ProductSummaryDto> ragProducts = hasMatch
                ? productService.getSummariesByIds(relevantIds, locale)
                : productService.getTrendingSummaries(props.getTrendingFallbackSize(), locale);

        String systemInstruction = buildSystemInstruction(ragProducts, hasMatch);
        List<GeminiChatClient.Content> contents = buildContents(request.getHistory(), message);
        List<GeminiChatClient.FunctionDecl> tools = recommendationTools();

        // Products surfaced by a tool call are the recommendations to show; they take precedence over RAG hits.
        LinkedHashMap<Long, ProductSummaryDto> toolProducts = new LinkedHashMap<>();
        String reply = null;
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            GeminiChatClient.Reply r = chatClient.generateWithTools(systemInstruction, contents, tools);
            if (r == null) break;
            if (!r.isCall()) {
                reply = r.text();
                break;
            }
            GeminiChatClient.FunctionCall call = r.functionCall();
            ToolOutcome outcome = dispatchTool(call, locale);
            for (ProductSummaryDto p : outcome.products()) {
                toolProducts.putIfAbsent(p.getId(), p);
            }
            contents.add(GeminiChatClient.Content.functionCall(call));
            contents.add(GeminiChatClient.Content.functionResponse(call.name(), outcome.response(), call.id()));
        }

        if (reply == null || reply.isBlank()) {
            reply = FALLBACK_REPLY;
        }
        List<ProductSummaryDto> products = toolProducts.isEmpty()
                ? ragProducts
                : new ArrayList<>(toolProducts.values());
        return ChatResponse.builder().reply(reply.trim()).products(products).build();
    }

    private record ToolOutcome(List<ProductSummaryDto> products, Map<String, Object> response) {
    }

    private ToolOutcome dispatchTool(GeminiChatClient.FunctionCall call, String locale) {
        String productName = call.args() == null ? null : call.args().path("product_name").asText(null);
        if (productName == null || productName.isBlank()) {
            return new ToolOutcome(List.of(), Map.of("error", "Missing product_name."));
        }
        Long productId = resolveProductId(productName);
        if (productId == null) {
            return new ToolOutcome(List.of(), Map.of("found", false, "query", productName));
        }
        List<ProductSummaryDto> recs = TOOL_FBT.equals(call.name())
                ? productService.getFrequentlyBoughtTogether(productId, REC_LIMIT, locale)
                : productService.getSimilarProducts(productId, REC_LIMIT, locale);

        List<Map<String, Object>> items = new ArrayList<>();
        for (ProductSummaryDto p : recs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", p.getName());
            BigDecimal price = p.getSalePrice() != null ? p.getSalePrice() : p.getBasePrice();
            if (price != null) item.put("price_vnd", price.longValue());
            if (p.getCategoryName() != null) item.put("category", p.getCategoryName());
            items.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("found", true);
        response.put("recommendations", items);
        return new ToolOutcome(recs, response);
    }

    private Long resolveProductId(String productName) {
        float[] vector = embeddingService.embedQuery(productName);
        List<ScoredProduct> hits = retrievalService.search(vector, 1);
        if (hits.isEmpty()) return null;
        ScoredProduct best = hits.get(0);
        return best.score() >= props.getScoreThreshold() ? best.productId() : null;
    }

    private List<GeminiChatClient.FunctionDecl> recommendationTools() {
        Map<String, Object> params = Map.of(
                "type", "object",
                "properties", Map.of("product_name", Map.of(
                        "type", "string",
                        "description", "Name of the product the user referred to.")),
                "required", List.of("product_name"));
        return List.of(
                new GeminiChatClient.FunctionDecl(TOOL_SIMILAR,
                        "Find products similar to a specific product the user named. Use when the user asks for items similar to, like, or alternatives to a named product.",
                        params),
                new GeminiChatClient.FunctionDecl(TOOL_FBT,
                        "Find products frequently bought together with a specific product the user named. Use when the user asks what pairs with or goes with a named product.",
                        params));
    }

    private List<GeminiChatClient.Content> buildContents(List<ChatTurn> history, String message) {
        List<GeminiChatClient.Content> contents = new ArrayList<>();
        if (history != null && !history.isEmpty()) {
            int maxMessages = Math.max(0, props.getHistoryMaxTurns()) * 2;
            int from = Math.max(0, history.size() - maxMessages);
            for (ChatTurn turn : history.subList(from, history.size())) {
                if (turn == null || turn.getContent() == null || turn.getContent().isBlank()) continue;
                contents.add(GeminiChatClient.Content.text(geminiRole(turn.getRole()), turn.getContent().trim()));
            }
        }
        // Gemini requires the conversation to start with a user turn.
        while (!contents.isEmpty() && "model".equals(contents.get(0).role())) {
            contents.remove(0);
        }
        contents.add(GeminiChatClient.Content.text("user", message));
        return contents;
    }

    private String geminiRole(String role) {
        return ("assistant".equalsIgnoreCase(role) || "model".equalsIgnoreCase(role)) ? "model" : "user";
    }

    private String buildSystemInstruction(List<ProductSummaryDto> products, boolean hasMatch) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Vesta's shopping assistant for an online fashion store.\n")
                .append("Rules:\n")
                .append("- Answer using the PRODUCTS and STORE POLICY below, plus any tool results. Never invent products, prices, or policies.\n")
                .append("- If the question is unrelated to shopping at Vesta, politely decline and steer back to fashion.\n")
                .append("- Refer to products by their exact name. Be concise, friendly, and helpful.\n")
                .append("- When the user asks for items similar to, or frequently bought with, a SPECIFIC named product, call the matching tool to fetch real recommendations instead of guessing.\n")
                .append("- If the user states a price limit (e.g. \"under 500k\"), only present products within it. ")
                .append("If none of the products below qualify, say so honestly instead of suggesting pricier ones.\n")
                .append("- Reply in the SAME language as the user's latest message.\n")
                .append("- Format the reply as plain text. For lists, put each item on its own line starting with \"- \". ")
                .append("Do NOT use Markdown emphasis such as ** or *, and do NOT use headings.\n\n");

        sb.append("STORE POLICY:\n")
                .append("- Shipping (flat per region): North ").append(vnd(shippingProperties.getNorth()))
                .append(", Central ").append(vnd(shippingProperties.getCentral()))
                .append(", South ").append(vnd(shippingProperties.getSouth()))
                .append(". Free shipping for orders from ").append(vnd(shippingProperties.getFreeThreshold())).append(".\n")
                .append("- Payment methods: COD, VNPAY, and Stripe (card).\n")
                .append("- Returns: within 7 days of delivery if items are unused with tags; refunds go to the original payment method.\n\n");

        if (hasMatch) {
            sb.append("PRODUCTS (top matches for the user's latest question):\n");
        } else {
            sb.append("No product directly matches the user's question. ")
                    .append("If the question is unrelated to Vesta shopping, briefly note you can only help with Vesta products. ")
                    .append("Then warmly invite the user to explore these trending items. Do NOT claim they match the query.\n")
                    .append("TRENDING NOW:\n");
        }
        if (products.isEmpty()) {
            sb.append("(No products available.)\n");
        } else {
            int i = 1;
            for (ProductSummaryDto p : products) {
                sb.append(i++).append(". ").append(p.getName());
                if (p.getSalePrice() != null) {
                    sb.append(" — ").append(vnd(p.getSalePrice())).append(" (was ").append(vnd(p.getBasePrice())).append(")");
                } else {
                    sb.append(" — ").append(vnd(p.getBasePrice()));
                }
                if (p.getCategoryName() != null) sb.append(", ").append(p.getCategoryName());
                if (p.getBrandName() != null) sb.append(", brand ").append(p.getBrandName());
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String vnd(BigDecimal value) {
        return value == null ? "?" : String.format("%,d VND", value.longValue());
    }
}
