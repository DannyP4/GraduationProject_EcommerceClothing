package com.uniform.store.service;

import java.util.List;

public interface EmbeddingService {

    float[] embedQuery(String text);

    List<float[]> embedDocuments(List<String> texts);
}
