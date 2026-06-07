package com.uniform.store.service.impl;

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

    @Test
    void chat_relevantProducts_groundsPromptWithProductNames_andReturnsProducts() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of(
                new ScoredProduct(1L, 0.8), new ScoredProduct(2L, 0.7)));
        when(productService.getSummariesByIds(eq(List.of(1L, 2L)), anyString()))
                .thenReturn(List.of(summary(1L, "Black Jacket"), summary(2L, "Navy Coat")));
        ArgumentCaptor<String> sys = ArgumentCaptor.forClass(String.class);
        when(chatClient.generate(sys.capture(), anyList())).thenReturn("Here are some options.");

        ChatRequest req = new ChatRequest();
        req.setMessage("warm jacket?");
        ChatResponse resp = service.chat(req, "en");

        assertThat(resp.getReply()).isEqualTo("Here are some options.");
        assertThat(resp.getProducts()).extracting(ProductSummaryDto::getName)
                .containsExactly("Black Jacket", "Navy Coat");
        assertThat(sys.getValue()).contains("Black Jacket").contains("Navy Coat").contains("STORE POLICY");
    }

    @Test
    void chat_belowThreshold_noProductsAttached_andSummariesNotFetched() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of(
                new ScoredProduct(1L, 0.1), new ScoredProduct(2L, 0.2)));
        when(chatClient.generate(anyString(), anyList())).thenReturn("Sorry, no good match.");

        ChatRequest req = new ChatRequest();
        req.setMessage("weather today?");
        ChatResponse resp = service.chat(req, "en");

        assertThat(resp.getProducts()).isEmpty();
        verify(productService, never()).getSummariesByIds(anyList(), anyString());
    }

    @Test
    void chat_blankGeneration_usesFallback() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of());
        when(chatClient.generate(anyString(), anyList())).thenReturn("");

        ChatRequest req = new ChatRequest();
        req.setMessage("hi");
        assertThat(service.chat(req, "vi").getReply()).isNotBlank();
    }

    @Test
    void chat_capsHistoryAndEndsWithCurrentUserMessage() {
        when(retrievalService.search(any(), anyInt())).thenReturn(List.of());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GeminiChatClient.Msg>> contents = ArgumentCaptor.forClass(List.class);
        when(chatClient.generate(anyString(), contents.capture())).thenReturn("ok");

        ChatRequest req = new ChatRequest();
        req.setMessage("current");
        List<ChatTurn> history = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            history.add(new ChatTurn("user", "u" + i));
            history.add(new ChatTurn("assistant", "a" + i));
        }
        req.setHistory(history);

        service.chat(req, "en");

        List<GeminiChatClient.Msg> sent = contents.getValue();
        assertThat(sent).hasSize(7); // historyMaxTurns=3 -> 6 history msgs + current
        assertThat(sent.get(0).role()).isEqualTo("user");
        assertThat(sent.get(sent.size() - 1).role()).isEqualTo("user");
        assertThat(sent.get(sent.size() - 1).text()).isEqualTo("current");
    }
}
