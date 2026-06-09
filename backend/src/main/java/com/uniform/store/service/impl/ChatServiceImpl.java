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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String FALLBACK_REPLY =
            "Sorry, I'm having trouble generating a response right now. Please try asking again or browse our product categories for inspiration!";

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
        List<ProductSummaryDto> products = hasMatch
                ? productService.getSummariesByIds(relevantIds, locale)
                : productService.getTrendingSummaries(props.getTrendingFallbackSize(), locale);

        String systemInstruction = buildSystemInstruction(products, hasMatch);
        List<GeminiChatClient.Msg> contents = buildContents(request.getHistory(), message);

        String reply = chatClient.generate(systemInstruction, contents);
        if (reply == null || reply.isBlank()) {
            reply = FALLBACK_REPLY;
        }
        return ChatResponse.builder().reply(reply.trim()).products(products).build();
    }

    private List<GeminiChatClient.Msg> buildContents(List<ChatTurn> history, String message) {
        List<GeminiChatClient.Msg> contents = new ArrayList<>();
        if (history != null && !history.isEmpty()) {
            int maxMessages = Math.max(0, props.getHistoryMaxTurns()) * 2;
            int from = Math.max(0, history.size() - maxMessages);
            for (ChatTurn turn : history.subList(from, history.size())) {
                if (turn == null || turn.getContent() == null || turn.getContent().isBlank()) continue;
                contents.add(new GeminiChatClient.Msg(geminiRole(turn.getRole()), turn.getContent().trim()));
            }
        }
        // Gemini requires the conversation to start with a user turn.
        while (!contents.isEmpty() && "model".equals(contents.get(0).role())) {
            contents.remove(0);
        }
        contents.add(new GeminiChatClient.Msg("user", message));
        return contents;
    }

    private String geminiRole(String role) {
        return ("assistant".equalsIgnoreCase(role) || "model".equalsIgnoreCase(role)) ? "model" : "user";
    }

    private String buildSystemInstruction(List<ProductSummaryDto> products, boolean hasMatch) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Vesta's shopping assistant for an online fashion store.\n")
                .append("Rules:\n")
                .append("- Answer ONLY using the PRODUCTS and STORE POLICY below. Never invent products, prices, or policies.\n")
                .append("- If the question is unrelated to shopping at Vesta, politely decline and steer back to fashion.\n")
                .append("- Refer to products by their exact name. Be concise, friendly, and helpful.\n")
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
