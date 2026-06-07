package com.uniform.store.service;

import java.util.List;

public interface RetrievalService {

    List<ScoredProduct> search(float[] queryVector, int topK);

    List<ScoredProduct> searchSimilarToProduct(Long productId, int topK);

    void reload();

    int indexSize();

    record ScoredProduct(Long productId, double score) {
    }
}
