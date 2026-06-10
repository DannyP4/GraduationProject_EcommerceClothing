package com.uniform.store.integration;

import com.uniform.store.config.GeminiProperties;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.service.EmbeddingService;
import com.uniform.store.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationIntegrationTest extends BaseIntegrationTest {

    @MockBean private EmbeddingService embeddingService;

    @Autowired private RetrievalService retrievalService;
    @Autowired private ProductRepository productRepository;
    @Autowired private GeminiProperties props;

    private String adminJwt;

    // Similar
    private Product seed;
    private Product near;
    private Product far;

    // FBT
    private Product p1;
    private Product p2;
    private Product p3;

    @BeforeEach
    void seed() {
        User admin = data.createAdmin("admin@vesta.test", "Admin1234");
        adminJwt = data.accessTokenFor(admin.getEmail());
        User buyer = data.createCustomer("buyer@vesta.test", "Pass1234");

        Brand brand = data.createBrand();
        Category category = data.createCategory();

        seed = named(data.createProduct(brand, category, new BigDecimal("250000")), "Seed Jacket");
        data.createVariant(seed, 10);
        data.createProductImage(seed, true, "kaggle/seed");

        near = named(data.createProduct(brand, category, new BigDecimal("260000")), "Near Jacket");
        data.createVariant(near, 10);
        data.createProductImage(near, true, "kaggle/near");

        far = named(data.createProduct(brand, category, new BigDecimal("90000")), "Far Shirt");
        data.createVariant(far, 10);
        data.createProductImage(far, true, "kaggle/far");

        when(embeddingService.embedDocuments(anyList())).thenAnswer(inv -> {
            List<String> docs = inv.getArgument(0);
            return docs.stream().map(d -> {
                if (d.contains("Seed")) return vec(1f, 0f);
                if (d.contains("Near")) return vec(0.8f, 0.6f);
                return vec(0f, 1f); // Far
            }).toList();
        });

        p1 = data.createProduct(brand, category, new BigDecimal("100000"));
        ProductVariant v1 = data.createVariant(p1, 10);
        p2 = data.createProduct(brand, category, new BigDecimal("120000"));
        ProductVariant v2 = data.createVariant(p2, 10);
        p3 = data.createProduct(brand, category, new BigDecimal("130000"));
        ProductVariant v3 = data.createVariant(p3, 10);

        Product p4 = data.createProduct(brand, category, new BigDecimal("140000"));
        ProductVariant v4 = data.createVariant(p4, 10);
        Product p5 = data.createProduct(brand, category, new BigDecimal("150000"));
        ProductVariant v5 = data.createVariant(p5, 0);
        Product p6 = data.createProduct(brand, category, new BigDecimal("160000"));
        p6.setIsActive(false);
        productRepository.save(p6);
        ProductVariant v6 = data.createVariant(p6, 10);

        Instant now = Instant.now();
        data.createOrderWithItems(buyer, List.of(v1, v2), OrderStatus.DELIVERED, now, PaymentProvider.COD);
        data.createOrderWithItems(buyer, List.of(v1, v2), OrderStatus.DELIVERED, now, PaymentProvider.COD);
        data.createOrderWithItems(buyer, List.of(v1, v3), OrderStatus.DELIVERED, now, PaymentProvider.COD);
        data.createOrderWithItems(buyer, List.of(v1, v4), OrderStatus.CANCELLED, now, PaymentProvider.COD);
        data.createOrderWithItems(buyer, List.of(v1, v5), OrderStatus.DELIVERED, now, PaymentProvider.COD);
        data.createOrderWithItems(buyer, List.of(v1, v6), OrderStatus.DELIVERED, now, PaymentProvider.COD);

        retrievalService.reload();
    }

    private Product named(Product p, String name) {
        p.setName(name);
        return productRepository.save(p);
    }

    private float[] vec(float a, float b) {
        float[] v = new float[props.getEmbeddingDim()];
        v[0] = a;
        v[1] = b;
        return v;
    }

    @Test
    void fbt_ranksCoOccurring_excludesSelfCancelledOutOfStockInactive() throws Exception {
        mockMvc.perform(get("/products/" + p1.getId() + "/frequently-bought-together"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(p2.getId()))
                .andExpect(jsonPath("$.data[1].id").value(p3.getId()));
    }

    @Test
    void fbt_respectsLimit() throws Exception {
        mockMvc.perform(get("/products/" + p1.getId() + "/frequently-bought-together?limit=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(p2.getId()));
    }

    @Test
    void fbt_coldStart_returnsEmpty() throws Exception {
        mockMvc.perform(get("/products/" + far.getId() + "/frequently-bought-together"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void similar_afterBackfill_returnsNeighborsExcludingSeed() throws Exception {
        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/" + seed.getId() + "/similar?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(near.getId()))
                .andExpect(jsonPath("$.data[1].id").value(far.getId()));
    }

    @Test
    void similar_productWithoutEmbedding_returnsEmpty() throws Exception {
        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/" + p1.getId() + "/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void similarToSet_aggregatesAcrossSeeds_excludesThem() throws Exception {
        mockMvc.perform(post("/admin/ai/embeddings/backfill")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/similar?ids=" + seed.getId() + "," + near.getId() + "&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(far.getId()));
    }
}
