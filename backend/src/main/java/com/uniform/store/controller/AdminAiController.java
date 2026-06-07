package com.uniform.store.controller;

import com.uniform.store.config.GeminiProperties;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.EmbeddingSearchResultDto;
import com.uniform.store.dto.response.EmbeddingStatusDto;
import com.uniform.store.entity.Product;
import com.uniform.store.repository.ProductEmbeddingRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.service.EmbeddingBackfillService;
import com.uniform.store.service.EmbeddingService;
import com.uniform.store.service.RetrievalService;
import com.uniform.store.service.RetrievalService.ScoredProduct;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/ai/embeddings")
@RequiredArgsConstructor
@Tag(name = "Admin AI")
@SecurityRequirement(name = "bearerAuth")
public class AdminAiController {

    private final EmbeddingBackfillService backfillService;
    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final ProductRepository productRepository;
    private final ProductEmbeddingRepository embeddingRepository;
    private final GeminiProperties props;

    @PostMapping("/backfill")
    @Operation(summary = "Embed catalog products (idempotent; force re-embeds; limit caps this pass for free-tier pacing)")
    public ApiResponse<EmbeddingBackfillService.BackfillReport> backfill(
            @RequestParam(defaultValue = "false") boolean force,
            @RequestParam(defaultValue = "0") int limit) {
        return ApiResponse.ok("Backfill pass complete", backfillService.backfill(force, limit));
    }

    @GetMapping("/status")
    @Operation(summary = "Embedding coverage and in-memory index size")
    public ApiResponse<EmbeddingStatusDto> status() {
        return ApiResponse.ok(EmbeddingStatusDto.builder()
                .embeddableProducts(productRepository.countEmbeddable())
                .storedEmbeddings(embeddingRepository.count())
                .indexSize(retrievalService.indexSize())
                .model(props.getEmbeddingModel())
                .dim(props.getEmbeddingDim())
                .build());
    }

    @GetMapping("/search")
    @Operation(summary = "Debug: embed a query and return top-K matching products")
    public ApiResponse<List<EmbeddingSearchResultDto>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int k) {
        float[] queryVector = embeddingService.embedQuery(q);
        List<ScoredProduct> hits = retrievalService.search(queryVector, k);
        if (hits.isEmpty()) return ApiResponse.ok(List.of());

        List<Long> ids = hits.stream().map(ScoredProduct::productId).toList();
        Map<Long, Product> byId = productRepository.findAllByIdInWithBrandAndCategory(ids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<EmbeddingSearchResultDto> results = hits.stream()
                .map(hit -> {
                    Product p = byId.get(hit.productId());
                    if (p == null) return null;
                    return EmbeddingSearchResultDto.builder()
                            .productId(p.getId())
                            .slug(p.getSlug())
                            .name(p.getName())
                            .basePrice(p.getBasePrice())
                            .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                            .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                            .score(hit.score())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
        return ApiResponse.ok(results);
    }
}
