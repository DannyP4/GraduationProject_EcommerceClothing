package com.uniform.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingStatusDto {
    private long embeddableProducts;
    private long storedEmbeddings;
    private int indexSize;
    private String model;
    private int dim;
}
