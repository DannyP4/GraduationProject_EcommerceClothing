package com.uniform.store.service.impl;

import com.uniform.store.repository.CategoryTranslationRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.ProductAttributeRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductTranslationRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.ReviewRepository;
import com.uniform.store.service.PricingService;
import com.uniform.store.service.RetrievalService;
import com.uniform.store.service.RetrievalService.ScoredProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock ProductImageRepository imageRepository;
    @Mock ProductAttributeRepository attributeRepository;
    @Mock ProductTranslationRepository productTranslationRepository;
    @Mock CategoryTranslationRepository categoryTranslationRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock PricingService pricingService;
    @Mock RetrievalService retrievalService;

    @InjectMocks ProductServiceImpl service;

    @Test
    void getSimilarProducts_mapsScoredIdsInOrder_andDelegatesToSummaries() {
        when(retrievalService.searchSimilarToProduct(1L, 10)).thenReturn(List.of(
                new ScoredProduct(5L, 0.91), new ScoredProduct(3L, 0.82)));
        when(productRepository.findAllByIdInWithBrandAndCategory(List.of(5L, 3L))).thenReturn(List.of());

        assertThat(service.getSimilarProducts(1L, 10, "en")).isEmpty();

        verify(retrievalService).searchSimilarToProduct(1L, 10);
        verify(productRepository).findAllByIdInWithBrandAndCategory(List.of(5L, 3L));
    }

    @Test
    void getSimilarProducts_clampsLimitToMax() {
        when(retrievalService.searchSimilarToProduct(1L, 20)).thenReturn(List.of());

        service.getSimilarProducts(1L, 999, "en");

        verify(retrievalService).searchSimilarToProduct(1L, 20);
    }

    @Test
    void getSimilarProducts_nonPositiveLimit_returnsEmpty_skipsRetrieval() {
        assertThat(service.getSimilarProducts(1L, 0, "en")).isEmpty();
        verifyNoInteractions(retrievalService);
    }

    @Test
    void getFrequentlyBoughtTogether_passesLimit_andPreservesRepoOrder() {
        when(orderItemRepository.findFrequentlyBoughtTogether(eq(1L), any(), eq(PageRequest.of(0, 5))))
                .thenReturn(List.of(7L, 9L));
        when(productRepository.findAllByIdInWithBrandAndCategory(List.of(7L, 9L))).thenReturn(List.of());

        assertThat(service.getFrequentlyBoughtTogether(1L, 5, "en")).isEmpty();

        verify(orderItemRepository).findFrequentlyBoughtTogether(eq(1L), any(), eq(PageRequest.of(0, 5)));
        verify(productRepository).findAllByIdInWithBrandAndCategory(List.of(7L, 9L));
    }

    @Test
    void getFrequentlyBoughtTogether_nonPositiveLimit_returnsEmpty_skipsRepo() {
        assertThat(service.getFrequentlyBoughtTogether(1L, -1, "en")).isEmpty();
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void getSimilarToProducts_aggregatesByBestScore_excludesSeeds() {
        when(retrievalService.searchSimilarToProduct(eq(1L), anyInt())).thenReturn(List.of(
                new ScoredProduct(5L, 0.90), new ScoredProduct(2L, 0.80)));
        when(retrievalService.searchSimilarToProduct(eq(2L), anyInt())).thenReturn(List.of(
                new ScoredProduct(5L, 0.95), new ScoredProduct(7L, 0.60)));
        when(productRepository.findAllByIdInWithBrandAndCategory(anyList())).thenReturn(List.of());

        service.getSimilarToProducts(List.of(1L, 2L), 10, "en");

        // 5 (best 0.95) before 7 (0.60); seeds 1 and 2 excluded.
        verify(productRepository).findAllByIdInWithBrandAndCategory(List.of(5L, 7L));
    }

    @Test
    void getSimilarToProducts_emptySeeds_returnsEmpty_skipsRetrieval() {
        assertThat(service.getSimilarToProducts(List.of(), 10, "en")).isEmpty();
        verifyNoInteractions(retrievalService);
    }
}
