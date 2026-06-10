package com.uniform.store.service.impl;

import com.uniform.store.config.GeminiProperties;
import com.uniform.store.repository.ProductEmbeddingRepository;
import com.uniform.store.repository.ProductEmbeddingRepository.EmbeddingRow;
import com.uniform.store.service.RetrievalService.ScoredProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetrievalServiceImplTest {

    private ProductEmbeddingRepository embeddingRepository;
    private RetrievalServiceImpl service;

    @BeforeEach
    void setup() {
        embeddingRepository = mock(ProductEmbeddingRepository.class);
        GeminiProperties props = new GeminiProperties();
        props.setEmbeddingDim(4);
        service = new RetrievalServiceImpl(embeddingRepository, props);

        when(embeddingRepository.findActiveEmbeddings()).thenReturn(List.of(
                row(1L, new float[]{1f, 0f, 0f, 0f}),
                row(2L, new float[]{0f, 1f, 0f, 0f}),
                row(3L, new float[]{0.8f, 0.6f, 0f, 0f})
        ));
        service.reload();
    }

    private EmbeddingRow row(long id, float[] vector) {
        return new EmbeddingRow() {
            public Long getProductId() { return id; }
            public Integer getDim() { return vector.length; }
            public byte[] getEmbedding() { return Vectors.toBytes(vector); }
        };
    }

    @Test
    void reload_buildsIndexFromActiveRows() {
        assertThat(service.indexSize()).isEqualTo(3);
    }

    @Test
    void search_ranksByCosineSimilarity() {
        List<ScoredProduct> hits = service.search(new float[]{1f, 0f, 0f, 0f}, 2);

        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).productId()).isEqualTo(1L);
        assertThat(hits.get(1).productId()).isEqualTo(3L);
        assertThat(hits.get(0).score()).isCloseTo(1.0, org.assertj.core.api.Assertions.within(1e-5));
    }

    @Test
    void searchSimilarToProduct_excludesSeedAndRanksRest() {
        List<ScoredProduct> hits = service.searchSimilarToProduct(1L, 5);

        assertThat(hits).extracting(ScoredProduct::productId).containsExactly(3L, 2L);
    }

    @Test
    void search_emptyIndexOrInvalidArgs_returnsEmpty() {
        when(embeddingRepository.findActiveEmbeddings()).thenReturn(List.of());
        service.reload();

        assertThat(service.search(new float[]{1f, 0f, 0f, 0f}, 5)).isEmpty();
    }

    @Test
    void searchSimilarToProduct_unknownProductId_returnsEmpty() {
        assertThat(service.searchSimilarToProduct(999L, 5)).isEmpty();
    }

    @Test
    void searchSimilarToProduct_nonPositiveTopK_returnsEmpty() {
        assertThat(service.searchSimilarToProduct(1L, 0)).isEmpty();
    }
}
