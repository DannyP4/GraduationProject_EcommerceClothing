package com.uniform.store.service.impl;

import com.uniform.store.config.GeminiProperties;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.enums.Gender;
import com.uniform.store.repository.ProductEmbeddingRepository;
import com.uniform.store.repository.ProductEmbeddingRepository.EmbeddingMetaRow;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.service.EmbeddingBackfillService.BackfillReport;
import com.uniform.store.service.EmbeddingService;
import com.uniform.store.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddingBackfillServiceImplTest {

    private ProductRepository productRepository;
    private ProductVariantRepository variantRepository;
    private ProductEmbeddingRepository embeddingRepository;
    private EmbeddingService embeddingService;
    private RetrievalService retrievalService;
    private ProductDocumentBuilder documentBuilder;
    private GeminiProperties props;
    private EmbeddingBackfillServiceImpl service;

    @BeforeEach
    void setup() {
        productRepository = mock(ProductRepository.class);
        variantRepository = mock(ProductVariantRepository.class);
        embeddingRepository = mock(ProductEmbeddingRepository.class);
        embeddingService = mock(EmbeddingService.class);
        retrievalService = mock(RetrievalService.class);
        documentBuilder = new ProductDocumentBuilder();
        props = new GeminiProperties();
        service = new EmbeddingBackfillServiceImpl(productRepository, variantRepository,
                embeddingRepository, embeddingService, retrievalService, documentBuilder, props);

        when(variantRepository.findActiveProductColors()).thenReturn(List.of());
        when(embeddingRepository.findByProductId(anyLong())).thenReturn(Optional.empty());
        when(embeddingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(embeddingService.embedDocuments(anyList())).thenAnswer(inv -> {
            List<String> docs = inv.getArgument(0);
            return docs.stream().map(d -> new float[]{1f, 0f}).toList();
        });
    }

    private Product product(long id, String name) {
        Product p = Product.builder()
                .name(name)
                .gender(Gender.MEN)
                .description("desc")
                .brand(Brand.builder().name("Nike").build())
                .category(Category.builder().name("Jackets").build())
                .build();
        p.setId(id);
        return p;
    }

    private EmbeddingMetaRow meta(long id, String hash, String model, int dim) {
        return new EmbeddingMetaRow() {
            public Long getProductId() { return id; }
            public String getContentHash() { return hash; }
            public String getModel() { return model; }
            public Integer getDim() { return dim; }
        };
    }

    @Test
    void backfill_freshCatalog_embedsAllAndReloadsIndex() {
        when(productRepository.findAllEmbeddable())
                .thenReturn(List.of(product(1L, "Alpha"), product(2L, "Beta")));
        when(embeddingRepository.findAllMeta()).thenReturn(List.of());

        BackfillReport report = service.backfill(false, 0);

        assertThat(report.totalEmbeddable()).isEqualTo(2);
        assertThat(report.embedded()).isEqualTo(2);
        assertThat(report.skipped()).isZero();
        assertThat(report.failed()).isZero();
        assertThat(report.remaining()).isZero();
        verify(embeddingService).embedDocuments(argThat(list -> list.size() == 2));
        verify(embeddingRepository, times(2)).save(any());
        verify(retrievalService).reload();
    }

    @Test
    void backfill_unchangedHash_skipsAndDoesNotCallGemini() {
        Product p = product(1L, "Alpha");
        String doc = documentBuilder.build(p, null);
        String hash = documentBuilder.contentHash(props.getEmbeddingModel(), props.getEmbeddingDim(), doc);
        when(productRepository.findAllEmbeddable()).thenReturn(List.of(p));
        when(embeddingRepository.findAllMeta())
                .thenReturn(List.of(meta(1L, hash, props.getEmbeddingModel(), props.getEmbeddingDim())));

        BackfillReport report = service.backfill(false, 0);

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.embedded()).isZero();
        assertThat(report.remaining()).isZero();
        verify(embeddingService, never()).embedDocuments(anyList());
        verify(retrievalService).reload();
    }

    @Test
    void backfill_forceTrue_reEmbedsEvenWhenHashMatches() {
        Product p = product(1L, "Alpha");
        String doc = documentBuilder.build(p, null);
        String hash = documentBuilder.contentHash(props.getEmbeddingModel(), props.getEmbeddingDim(), doc);
        when(productRepository.findAllEmbeddable()).thenReturn(List.of(p));
        when(embeddingRepository.findAllMeta())
                .thenReturn(List.of(meta(1L, hash, props.getEmbeddingModel(), props.getEmbeddingDim())));

        BackfillReport report = service.backfill(true, 0);

        assertThat(report.embedded()).isEqualTo(1);
        assertThat(report.skipped()).isZero();
        verify(embeddingService).embedDocuments(anyList());
    }

    @Test
    void backfill_limit_capsThisPassAndReportsRemaining() {
        when(productRepository.findAllEmbeddable())
                .thenReturn(List.of(product(1L, "Alpha"), product(2L, "Beta"), product(3L, "Gamma")));
        when(embeddingRepository.findAllMeta()).thenReturn(List.of());

        BackfillReport report = service.backfill(false, 2);

        assertThat(report.totalEmbeddable()).isEqualTo(3);
        assertThat(report.embedded()).isEqualTo(2);
        assertThat(report.remaining()).isEqualTo(1);
        verify(embeddingService).embedDocuments(argThat(list -> list.size() == 2));
    }
}
