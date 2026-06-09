package com.uniform.store.integration;

import com.uniform.store.config.GeminiProperties;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.service.EmbeddingBackfillService;
import com.uniform.store.service.EmbeddingService;
import com.uniform.store.service.impl.GeminiChatClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatIntegrationTest extends BaseIntegrationTest {

    @MockBean private EmbeddingService embeddingService;
    @MockBean private GeminiChatClient chatClient;

    @Autowired private GeminiProperties props;
    @Autowired private EmbeddingBackfillService backfillService;
    @Autowired private com.uniform.store.repository.ProductRepository productRepository;

    private Product alpha;

    @BeforeEach
    void seed() {
        Brand brand = data.createBrand();
        Category category = data.createCategory();

        alpha = data.createProduct(brand, category, new BigDecimal("250000"));
        alpha.setName("Alpha Winter Jacket");
        productRepository.save(alpha);
        data.createVariant(alpha, 10);
        data.createProductImage(alpha, true, "uniform/products/kaggle-alpha");

        Product beta = data.createProduct(brand, category, new BigDecimal("180000"));
        beta.setName("Beta Summer Shirt");
        productRepository.save(beta);
        data.createVariant(beta, 10);
        data.createProductImage(beta, true, "uniform/products/kaggle-beta");

        when(embeddingService.embedDocuments(anyList())).thenAnswer(inv -> {
            List<String> docs = inv.getArgument(0);
            return docs.stream().map(d -> d.contains("Alpha") ? unit(0) : unit(1)).toList();
        });
        backfillService.backfill(false, 0);

        when(chatClient.generate(anyString(), anyList())).thenReturn("Sure, here are some options for you.");
    }

    private float[] unit(int axis) {
        float[] v = new float[props.getEmbeddingDim()];
        v[axis] = 1f;
        return v;
    }

    @Test
    void chat_isPublic_returnsGroundedReplyAndMatchingProducts() throws Exception {
        when(embeddingService.embedQuery(anyString())).thenReturn(unit(0)); // matches alpha

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"warm jacket for winter?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("Sure, here are some options for you."))
                .andExpect(jsonPath("$.data.products.length()").value(1))
                .andExpect(jsonPath("$.data.products[0].name").value("Alpha Winter Jacket"));
    }

    @Test
    void chat_offTopicQuery_fallsBackToTrendingProducts() throws Exception {
        when(embeddingService.embedQuery(anyString())).thenReturn(unit(2)); // orthogonal to all -> no match

        // no sales seeded, so trending falls back to newest active products
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"what is the weather today?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products.length()").value(2));
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
