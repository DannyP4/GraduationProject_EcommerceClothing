package com.uniform.store.service.impl;

import com.uniform.store.config.GeminiProperties;
import com.uniform.store.repository.ProductEmbeddingRepository;
import com.uniform.store.service.RetrievalService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalServiceImpl.class);

    private final ProductEmbeddingRepository embeddingRepository;
    private final GeminiProperties props;

    private volatile Index index = new Index(new long[0], new float[0][]);

    private record Index(long[] ids, float[][] vectors) {
    }

    @PostConstruct
    void init() {
        try {
            reload();
        } catch (Exception e) {
            log.warn("Embedding index initial load failed (continuing with empty index): {}", e.getMessage());
        }
    }

    @Override
    public void reload() {
        int dim = props.getEmbeddingDim();
        List<ProductEmbeddingRepository.EmbeddingRow> rows = embeddingRepository.findActiveEmbeddings();
        long[] ids = new long[rows.size()];
        float[][] vectors = new float[rows.size()][];
        int n = 0;
        for (ProductEmbeddingRepository.EmbeddingRow row : rows) {
            if (row.getDim() == null || row.getDim() != dim) continue;
            float[] v = Vectors.toFloats(row.getEmbedding());
            if (v.length != dim) continue;
            ids[n] = row.getProductId();
            vectors[n] = v;
            n++;
        }
        if (n != rows.size()) {
            long[] trimmedIds = new long[n];
            float[][] trimmedVectors = new float[n][];
            System.arraycopy(ids, 0, trimmedIds, 0, n);
            System.arraycopy(vectors, 0, trimmedVectors, 0, n);
            ids = trimmedIds;
            vectors = trimmedVectors;
        }
        this.index = new Index(ids, vectors);
        log.info("Embedding index loaded: {} vectors (dim={})", n, dim);
    }

    @Override
    public int indexSize() {
        return index.ids().length;
    }

    @Override
    public List<ScoredProduct> search(float[] queryVector, int topK) {
        Index idx = this.index;
        if (idx.ids().length == 0 || queryVector == null || queryVector.length == 0 || topK <= 0) {
            return List.of();
        }
        float[] query = Vectors.normalize(queryVector);
        List<ScoredProduct> scored = new ArrayList<>(idx.ids().length);
        for (int i = 0; i < idx.ids().length; i++) {
            scored.add(new ScoredProduct(idx.ids()[i], Vectors.dot(query, idx.vectors()[i])));
        }
        scored.sort(Comparator.comparingDouble(ScoredProduct::score).reversed());
        return new ArrayList<>(scored.subList(0, Math.min(topK, scored.size())));
    }

    @Override
    public List<ScoredProduct> searchSimilarToProduct(Long productId, int topK) {
        if (productId == null || topK <= 0) return List.of();
        Index idx = this.index;
        float[] seed = null;
        for (int i = 0; i < idx.ids().length; i++) {
            if (idx.ids()[i] == productId) {
                seed = idx.vectors()[i];
                break;
            }
        }
        if (seed == null) return List.of();
        List<ScoredProduct> scored = new ArrayList<>(idx.ids().length);
        for (int i = 0; i < idx.ids().length; i++) {
            if (idx.ids()[i] == productId) continue;
            scored.add(new ScoredProduct(idx.ids()[i], Vectors.dot(seed, idx.vectors()[i])));
        }
        scored.sort(Comparator.comparingDouble(ScoredProduct::score).reversed());
        return new ArrayList<>(scored.subList(0, Math.min(topK, scored.size())));
    }
}
