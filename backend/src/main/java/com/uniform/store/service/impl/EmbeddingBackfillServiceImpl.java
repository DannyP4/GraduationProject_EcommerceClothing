package com.uniform.store.service.impl;

import com.uniform.store.config.GeminiProperties;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductEmbedding;
import com.uniform.store.repository.ProductEmbeddingRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.service.EmbeddingBackfillService;
import com.uniform.store.service.EmbeddingService;
import com.uniform.store.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmbeddingBackfillServiceImpl implements EmbeddingBackfillService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingBackfillServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductEmbeddingRepository embeddingRepository;
    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final ProductDocumentBuilder documentBuilder;
    private final GeminiProperties props;

    @Override
    public BackfillReport backfill(boolean force, int limit) {
        String model = props.getEmbeddingModel();
        int dim = props.getEmbeddingDim();

        List<Product> products = productRepository.findAllEmbeddable();
        Map<Long, List<String>> colorsByProduct = loadColors();
        Map<Long, ProductEmbeddingRepository.EmbeddingMetaRow> metaByProduct = new HashMap<>();
        for (ProductEmbeddingRepository.EmbeddingMetaRow m : embeddingRepository.findAllMeta()) {
            metaByProduct.put(m.getProductId(), m);
        }

        List<Pending> pending = new ArrayList<>();
        int skipped = 0;
        for (Product product : products) {
            String document = documentBuilder.build(product, colorsByProduct.get(product.getId()));
            String hash = documentBuilder.contentHash(model, dim, document);
            ProductEmbeddingRepository.EmbeddingMetaRow meta = metaByProduct.get(product.getId());
            boolean upToDate = meta != null
                    && hash.equals(meta.getContentHash())
                    && model.equals(meta.getModel())
                    && meta.getDim() != null && meta.getDim() == dim;
            if (!force && upToDate) {
                skipped++;
                continue;
            }
            pending.add(new Pending(product, document, hash));
        }

        int toProcess = (limit > 0) ? Math.min(limit, pending.size()) : pending.size();
        List<Pending> slice = pending.subList(0, toProcess);

        int embedded = 0;
        int failed = 0;
        int batchSize = Math.max(1, props.getBatchSize());
        for (int i = 0; i < slice.size(); i += batchSize) {
            List<Pending> chunk = slice.subList(i, Math.min(i + batchSize, slice.size()));
            try {
                List<float[]> vectors = embeddingService.embedDocuments(
                        chunk.stream().map(Pending::document).toList());
                for (int j = 0; j < chunk.size(); j++) {
                    persist(chunk.get(j), vectors.get(j), model);
                }
                embedded += chunk.size();
            } catch (Exception e) {
                failed += chunk.size();
                log.warn("Embedding backfill chunk [{}..{}] failed: {}", i, i + chunk.size(), e.getMessage());
            }
        }

        retrievalService.reload();
        int remaining = pending.size() - embedded;
        log.info("Embedding backfill pass: embeddable={} embedded={} skipped={} failed={} remaining={}",
                products.size(), embedded, skipped, failed, remaining);
        return new BackfillReport(products.size(), embedded, skipped, failed, remaining);
    }

    private void persist(Pending pending, float[] vector, String model) {
        ProductEmbedding entity = embeddingRepository.findByProductId(pending.product().getId())
                .orElseGet(ProductEmbedding::new);
        entity.setProduct(pending.product());
        entity.setModel(model);
        entity.setDim(vector.length);
        entity.setContentHash(pending.hash());
        entity.setEmbedding(Vectors.toBytes(vector));
        embeddingRepository.save(entity);
    }

    private Map<Long, List<String>> loadColors() {
        Map<Long, LinkedHashSet<String>> grouped = new HashMap<>();
        for (ProductVariantRepository.ProductColorView row : variantRepository.findActiveProductColors()) {
            if (row.getColor() == null || row.getColor().isBlank()) continue;
            grouped.computeIfAbsent(row.getProductId(), k -> new LinkedHashSet<>()).add(row.getColor());
        }
        Map<Long, List<String>> out = new HashMap<>();
        grouped.forEach((id, colors) -> out.put(id, new ArrayList<>(colors)));
        return out;
    }

    private record Pending(Product product, String document, String hash) {
    }
}
