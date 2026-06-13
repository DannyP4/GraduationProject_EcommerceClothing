package com.uniform.store.service.impl;

import com.uniform.store.dto.response.AutoTranslateReport;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductTranslation;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.BrandTranslationRepository;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.CategoryTranslationRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductTranslationRepository;
import com.uniform.store.service.TranslationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogTranslationServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductTranslationRepository productTranslationRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryTranslationRepository categoryTranslationRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private BrandTranslationRepository brandTranslationRepository;
    @Mock private TranslationProvider translationProvider;

    @InjectMocks private CatalogTranslationServiceImpl service;

    private Product product;

    @BeforeEach
    void setup() {
        product = Product.builder().slug("tee").name("Tee").description("Soft cotton").build();
        product.setId(1L);
    }

    @Test
    void run_rejectsEnglishLocale() {
        assertThatThrownBy(() -> service.run("en", null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vi");
        verify(translationProvider, never()).translate(anyList(), any());
    }

    @Test
    void run_providerDisabled_throws() {
        when(translationProvider.isEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.run("vi", null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DeepL");
    }

    @Test
    void run_translatesPendingProduct_savesAutoTranslatedRow() {
        when(translationProvider.isEnabled()).thenReturn(true);
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 200), 1));
        when(productTranslationRepository.findByProductIdInAndLocale(any(), eq("vi"))).thenReturn(List.of());
        when(translationProvider.translate(anyList(), eq("vi"))).thenReturn(List.of("Áo thun", "Cotton mềm"));
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(brandRepository.findAll()).thenReturn(List.of());

        AutoTranslateReport report = service.run("vi", null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductTranslation>> captor = ArgumentCaptor.forClass(List.class);
        verify(productTranslationRepository).saveAll(captor.capture());
        ProductTranslation saved = captor.getValue().get(0);
        assertThat(saved.getLocale()).isEqualTo("vi");
        assertThat(saved.getName()).isEqualTo("Áo thun");
        assertThat(saved.getDescription()).isEqualTo("Cotton mềm");
        assertThat(saved.getIsAutoTranslated()).isTrue();
        assertThat(report.productsTranslated()).isEqualTo(1);
        assertThat(report.stoppedEarly()).isFalse();
    }

    @Test
    void run_skipsProductThatAlreadyHasTranslation() {
        ProductTranslation existing = ProductTranslation.builder()
                .product(product).locale("vi").name("Áo cũ").build();
        when(translationProvider.isEnabled()).thenReturn(true);
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 200), 1));
        when(productTranslationRepository.findByProductIdInAndLocale(any(), eq("vi"))).thenReturn(List.of(existing));
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(brandRepository.findAll()).thenReturn(List.of());

        AutoTranslateReport report = service.run("vi", null, null);

        verify(translationProvider, never()).translate(anyList(), any());
        verify(productTranslationRepository, never()).saveAll(any());
        assertThat(report.productsTranslated()).isZero();
        assertThat(report.skippedExisting()).isEqualTo(1);
    }

    @Test
    void run_respectsCharBudget_stopsEarlyWithoutCallingProvider() {
        when(translationProvider.isEnabled()).thenReturn(true);
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 200), 1));
        when(productTranslationRepository.findByProductIdInAndLocale(any(), eq("vi"))).thenReturn(List.of());

        AutoTranslateReport report = service.run("vi", null, 5L); // "Tee"+"Soft cotton" = 14 chars > 5

        assertThat(report.stoppedEarly()).isTrue();
        assertThat(report.productsTranslated()).isZero();
        verify(translationProvider, never()).translate(anyList(), any());
        verify(productTranslationRepository, never()).saveAll(any());
    }
}
