package com.uniform.store.integration;

import com.uniform.store.config.GeminiProperties;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.User;
import com.uniform.store.repository.ProductEmbeddingRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.service.EmbeddingService;
import com.uniform.store.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiEmbeddingIntegrationTest extends BaseIntegrationTest {

    @MockBean private EmbeddingService embeddingService;

    @Autowired private RetrievalService retrievalService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductEmbeddingRepository embeddingRepository;
    @Autowired private GeminiProperties props;

    private String adminJwt;
    private String customerJwt;
    private Product alpha;
    private Product beta;
    private Product gamma;

    @BeforeEach
    void seed() {
        User admin = data.createAdmin("admin@vesta.test", "Admin1234");
        adminJwt = data.accessTokenFor(admin.getEmail());
        User customer = data.createCustomer("user@vesta.test", "Pass1234");
        customerJwt = data.accessTokenFor(customer.getEmail());

        Brand brand = data.createBrand();
        Category category = data.createCategory();

        // alpha + beta have real images -> embeddable
        alpha = data.createProduct(brand, category, new BigDecimal("250000"));
        alpha.setName("Alpha Winter Jacket");
        productRepository.save(alpha);
        data.createVariant(alpha, 10);
        data.createProductImage(alpha, true, "uniform/products/kaggle-alpha");

        beta = data.createProduct(brand, category, new BigDecimal("180000"));
        beta.setName("Beta Summer Shirt");
        productRepository.save(beta);
        data.createVariant(beta, 10);
        data.createProductImage(beta, true, "uniform/products/kaggle-beta");

        // gamma is a demo-style product: placeholder image, NULL public_id, excluded from scope
        gamma = data.createProduct(brand, category, new BigDecimal("90000"));
        gamma.setName("Gamma Placeholder");
        productRepository.save(gamma);
        data.createVariant(gamma, 10);
        data.createProductImage(gamma, true, null);

        when(embeddingService.embedDocuments(anyList())).thenAnswer(inv -> {
            List<String> docs = inv.getArgument(0);
            return docs.stream().map(d -> d.contains("Alpha") ? unit(0) : unit(1)).toList();
        });
        when(embeddingService.embedQuery(anyString())).thenAnswer(inv -> {
            String q = inv.getArgument(0);
            return q.contains("Alpha") ? unit(0) : unit(1);
        });

        retrievalService.reload();
    }

    private float[] unit(int axis) {
        float[] v = new float[props.getEmbeddingDim()];
        v[axis] = 1f;
        return v;
    }

    @Test
    void status_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/admin/ai/embeddings/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void backfill_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void backfill_embedsOnlyProductsWithRealImages_thenIsIdempotent() throws Exception {
        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEmbeddable").value(2))
                .andExpect(jsonPath("$.data.embedded").value(2))
                .andExpect(jsonPath("$.data.skipped").value(0))
                .andExpect(jsonPath("$.data.remaining").value(0));

        org.assertj.core.api.Assertions.assertThat(embeddingRepository.count()).isEqualTo(2);

        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.embedded").value(0))
                .andExpect(jsonPath("$.data.skipped").value(2));
    }

    @Test
    void backfill_limit_pacesAcrossPasses() throws Exception {
        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.embedded").value(1))
                .andExpect(jsonPath("$.data.remaining").value(1));

        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.embedded").value(1))
                .andExpect(jsonPath("$.data.skipped").value(1))
                .andExpect(jsonPath("$.data.remaining").value(0));
    }

    @Test
    void search_returnsProductsRankedByCosine_andExcludesPlaceholderProducts() throws Exception {
        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/ai/embeddings/search")
                        .param("q", "Alpha jacket for winter")
                        .param("k", "10")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].productId").value(alpha.getId()))
                .andExpect(jsonPath("$.data[0].name").value("Alpha Winter Jacket"));
    }

    @Test
    void status_reportsCoverageAndIndexSize() throws Exception {
        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/ai/embeddings/status")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.embeddableProducts").value(2))
                .andExpect(jsonPath("$.data.storedEmbeddings").value(2))
                .andExpect(jsonPath("$.data.indexSize").value(2))
                .andExpect(jsonPath("$.data.dim").value(props.getEmbeddingDim()));
    }
}
