package com.uniform.store.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniform.store.config.GeminiProperties;
import com.uniform.store.config.ShippingProperties;
import com.uniform.store.dto.request.ChatRequest;
import com.uniform.store.dto.request.ChatTurn;
import com.uniform.store.dto.response.ChatResponse;
import com.uniform.store.dto.response.ProductSummaryDto;
import com.uniform.store.service.EmbeddingService;
import com.uniform.store.service.ProductService;
import com.uniform.store.service.RetrievalService;
import com.uniform.store.service.RetrievalService.ScoredProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    private EmbeddingService embeddingService;
    private RetrievalService retrievalService;
    private ProductService productService;
    private GeminiChatClient chatClient;
    private GeminiProperties props;
    private ChatServiceImpl service;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setup() {
        embeddingService = mock(EmbeddingService.class);
        retrievalService = mock(RetrievalService.class);
        productService = mock(ProductService.class);
        chatClient = mock(GeminiChatClient.class);
        props = new GeminiProperties();
        service = new ChatServiceImpl(embeddingService, retrievalService, productService, chatClient,
                props, new ShippingProperties());
        when(embeddingService.embedQuery(anyString())).thenReturn(new float[]{1f, 0f});
    }

    private ProductSummaryDto summary(long id, String name) {
        return ProductSummaryDto.builder().id(id).slug("s" + id).name(name)
                .basePrice(new BigDecimal("250000")).currency("VND")
                .brandName("Nike").categoryName("Jackets").build();
    }

    private GeminiChatClient.Reply text(String t) {
        return new GeminiChatClient.Reply(t, null);
    }

    private GeminiChatClient.Reply call(String name, String productName) {
        return new GeminiChatClient.Reply(null, new GeminiChatClient.FunctionCall(
                name, MAPPER.createObjectNode().put("product_name", productName), "fc-1"));
    }

    @Test
    void chat_relevantProducts_groundsPromptWithProductNames_andReturnsProducts() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of(
                new ScoredProduct(1L, 0.8), new ScoredProduct(2L, 0.7)));
        when(productService.getSummariesByIds(eq(List.of(1L, 2L)), anyString()))
                .thenReturn(List.of(summary(1L, "Black Jacket"), summary(2L, "Navy Coat")));
        ArgumentCaptor<String> sys = ArgumentCaptor.forClass(String.class);
        when(chatClient.generateWithTools(sys.capture(), anyList(), anyList())).thenReturn(text("Here are some options."));

        ChatRequest req = new ChatRequest();
        req.setMessage("warm jacket?");
        ChatResponse resp = service.chat(req, "en");

        assertThat(resp.getReply()).isEqualTo("Here are some options.");
        assertThat(resp.getProducts()).extracting(ProductSummaryDto::getName)
                .containsExactly("Black Jacket", "Navy Coat");
        assertThat(sys.getValue()).contains("Black Jacket").contains("Navy Coat").contains("STORE POLICY");
    }

    @Test
    void chat_belowThreshold_usesTrendingFallback_notSummaries() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of(
                new ScoredProduct(1L, 0.1), new ScoredProduct(2L, 0.2)));
        when(productService.getTrendingSummaries(anyInt(), anyString()))
                .thenReturn(List.of(summary(9L, "Best Seller Tee")));
        ArgumentCaptor<String> sys = ArgumentCaptor.forClass(String.class);
        when(chatClient.generateWithTools(sys.capture(), anyList(), anyList()))
                .thenReturn(text("No exact match, but check these out."));

        ChatRequest req = new ChatRequest();
        req.setMessage("weather today?");
        ChatResponse resp = service.chat(req, "en");

        assertThat(resp.getProducts()).extracting(ProductSummaryDto::getName).containsExactly("Best Seller Tee");
        assertThat(sys.getValue()).contains("TRENDING NOW");
        verify(productService, never()).getSummariesByIds(anyList(), anyString());
        verify(productService).getTrendingSummaries(anyInt(), anyString());
    }

    @Test
    void chat_blankGeneration_usesFallback() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of());
        when(chatClient.generateWithTools(anyString(), anyList(), anyList())).thenReturn(text(""));

        ChatRequest req = new ChatRequest();
        req.setMessage("hi");
        assertThat(service.chat(req, "vi").getReply()).isNotBlank();
    }

    @Test
    void chat_capsHistoryAndEndsWithCurrentUserMessage() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GeminiChatClient.Content>> contents = ArgumentCaptor.forClass(List.class);
        when(chatClient.generateWithTools(anyString(), contents.capture(), anyList())).thenReturn(text("ok"));

        ChatRequest req = new ChatRequest();
        req.setMessage("current");
        List<ChatTurn> history = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            history.add(new ChatTurn("user", "u" + i));
            history.add(new ChatTurn("assistant", "a" + i));
        }
        req.setHistory(history);

        service.chat(req, "en");

        List<GeminiChatClient.Content> sent = contents.getValue();
        assertThat(sent).hasSize(7); // historyMaxTurns=3 -> 6 history msgs + current
        assertThat(sent.get(0).role()).isEqualTo("user");
        assertThat(sent.get(sent.size() - 1).role()).isEqualTo("user");
        assertThat(sent.get(sent.size() - 1).text()).isEqualTo("current");
    }

    @Test
    void chat_toolCall_resolvesProduct_returnsRecommendationProducts() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of(new ScoredProduct(5L, 0.9)));
        when(productService.getSummariesByIds(anyList(), anyString())).thenReturn(List.of(summary(5L, "Seed Product")));
        when(productService.getSimilarProducts(eq(5L), anyInt(), anyString()))
                .thenReturn(List.of(summary(11L, "Similar A"), summary(12L, "Similar B")));
        when(chatClient.generateWithTools(anyString(), anyList(), anyList()))
                .thenReturn(call(TOOL_SIMILAR, "Seed Product"))
                .thenReturn(text("Here are similar items."));

        ChatRequest req = new ChatRequest();
        req.setMessage("anything similar to Seed Product?");
        ChatResponse resp = service.chat(req, "en");

        assertThat(resp.getReply()).isEqualTo("Here are similar items.");
        assertThat(resp.getProducts()).extracting(ProductSummaryDto::getName)
                .containsExactly("Similar A", "Similar B");
        verify(productService).getSimilarProducts(eq(5L), anyInt(), anyString());
    }

    @Test
    void chat_toolCall_unresolvedProduct_fallsBackToRagProducts() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of());
        when(productService.getTrendingSummaries(anyInt(), anyString())).thenReturn(List.of(summary(9L, "Trending Tee")));
        when(chatClient.generateWithTools(anyString(), anyList(), anyList()))
                .thenReturn(call(TOOL_SIMILAR, "Ghost Product"))
                .thenReturn(text("I couldn't find that product."));

        ChatRequest req = new ChatRequest();
        req.setMessage("similar to Ghost Product?");
        ChatResponse resp = service.chat(req, "en");

        assertThat(resp.getReply()).isEqualTo("I couldn't find that product.");
        assertThat(resp.getProducts()).extracting(ProductSummaryDto::getName).containsExactly("Trending Tee");
        verify(productService, never()).getSimilarProducts(anyLong(), anyInt(), anyString());
    }

    @Test
    void chat_heightWeight_advisesSize_keepsRagProducts() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of(new ScoredProduct(7L, 0.8)));
        when(productService.getSummariesByIds(anyList(), anyString())).thenReturn(List.of(summary(7L, "Red Tee")));
        GeminiChatClient.FunctionCall sizeCall = new GeminiChatClient.FunctionCall(
                "recommend_clothing_size",
                MAPPER.createObjectNode().put("height_cm", 170).put("weight_kg", 65), "fc-size");
        when(chatClient.generateWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new GeminiChatClient.Reply(null, sizeCall))
                .thenReturn(text("Size L for a snug fit, XL for comfort. Here are red tees under 500k:"));

        ChatRequest req = new ChatRequest();
        req.setMessage("cao 1m70 nang 65kg, ao do duoi 500k?");
        ChatResponse resp = service.chat(req, "vi");

        assertThat(resp.getReply()).contains("Size L");
        assertThat(resp.getProducts()).extracting(ProductSummaryDto::getName).containsExactly("Red Tee");
        verify(productService, never()).getSimilarProducts(anyLong(), anyInt(), anyString());
    }

    private static final String TOOL_SIMILAR = "find_similar_products";
}
