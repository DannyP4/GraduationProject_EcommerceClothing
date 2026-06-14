package com.uniform.store.service.impl;

import com.uniform.store.dto.request.TryOnCreateRequest;
import com.uniform.store.dto.response.CloudinaryUploadResult;
import com.uniform.store.dto.response.TryOnJobDto;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.TryOnJob;
import com.uniform.store.entity.User;
import com.uniform.store.enums.TryOnStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.exception.TryOnException;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.TryOnJobRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.CloudinaryService;
import com.uniform.store.service.VirtualTryOnProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirtualTryOnServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductImageRepository productImageRepository;
    @Mock TryOnJobRepository tryOnJobRepository;
    @Mock VirtualTryOnProvider provider;
    @Mock CloudinaryService cloudinaryService;

    VirtualTryOnServiceImpl service;

    User user;
    Product product;

    @BeforeEach
    void setUp() {
        service = new VirtualTryOnServiceImpl(userRepository, productRepository, productImageRepository,
                tryOnJobRepository, provider, cloudinaryService);

        user = User.builder().email("u@uniform.test").build();
        user.setId(2L);
        product = Product.builder().slug("tee").name("Tee").isActive(true).build();
        product.setId(7L);
    }

    private TryOnCreateRequest request() {
        TryOnCreateRequest req = new TryOnCreateRequest();
        req.setProductId(7L);
        req.setUserImageUrl("https://cdn/user.jpg");
        return req;
    }

    @Test
    void createJob_providerDisabled_throws() {
        when(provider.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.createJob("u@uniform.test", request()))
                .isInstanceOf(TryOnException.class);
        verify(tryOnJobRepository, never()).save(any());
    }

    @Test
    void createJob_submitsAndPersistsProcessingJob() {
        when(provider.isEnabled()).thenReturn(true);
        when(provider.name()).thenReturn("FAL_FASHN");
        when(userRepository.findByEmail("u@uniform.test")).thenReturn(Optional.of(user));
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(7L))
                .thenReturn(List.of(image("https://cdn/shirt.jpg")));
        when(tryOnJobRepository
                .findFirstByUserIdAndProductIdAndUserImageUrlAndGarmentPhotoTypeAndStatusOrderByCreatedAtDesc(
                        eq(2L), eq(7L), any(), eq("auto"), eq(TryOnStatus.SUCCEEDED)))
                .thenReturn(Optional.empty());
        when(provider.submit(any(), any(), any(), any()))
                .thenReturn(new VirtualTryOnProvider.Submission("req1", "https://resp/1"));
        when(tryOnJobRepository.save(any())).thenAnswer(inv -> {
            TryOnJob j = inv.getArgument(0);
            j.setId(99L);
            return j;
        });

        TryOnJobDto dto = service.createJob("u@uniform.test", request());

        assertThat(dto.getId()).isEqualTo(99L);
        assertThat(dto.getStatus()).isEqualTo("PROCESSING");
        assertThat(dto.isCached()).isFalse();
        assertThat(dto.getGarmentImageUrl()).isEqualTo("https://cdn/shirt.jpg");
        verify(provider).submit("https://cdn/user.jpg", "https://cdn/shirt.jpg", "auto", null);
    }

    @Test
    void createJob_cacheHit_returnsCachedAndSkipsProvider() {
        when(provider.isEnabled()).thenReturn(true);
        when(userRepository.findByEmail("u@uniform.test")).thenReturn(Optional.of(user));
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(7L))
                .thenReturn(List.of(image("https://cdn/shirt.jpg")));

        TryOnJob cached = TryOnJob.builder()
                .product(product).status(TryOnStatus.SUCCEEDED)
                .resultImageUrl("https://cdn/result.png").build();
        cached.setId(50L);
        when(tryOnJobRepository
                .findFirstByUserIdAndProductIdAndUserImageUrlAndGarmentPhotoTypeAndStatusOrderByCreatedAtDesc(
                        eq(2L), eq(7L), any(), eq("auto"), eq(TryOnStatus.SUCCEEDED)))
                .thenReturn(Optional.of(cached));

        TryOnJobDto dto = service.createJob("u@uniform.test", request());

        assertThat(dto.getId()).isEqualTo(50L);
        assertThat(dto.isCached()).isTrue();
        assertThat(dto.getResultImageUrl()).isEqualTo("https://cdn/result.png");
        verify(provider, never()).submit(any(), any(), any(), any());
        verify(tryOnJobRepository, never()).save(any());
    }

    @Test
    void createJob_productWithoutImage_throwsBadRequest() {
        when(provider.isEnabled()).thenReturn(true);
        when(userRepository.findByEmail("u@uniform.test")).thenReturn(Optional.of(user));
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(7L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.createJob("u@uniform.test", request()))
                .isInstanceOf(BadRequestException.class);
        verify(provider, never()).submit(any(), any(), any(), any());
    }

    @Test
    void getJob_processingPollSucceeds_uploadsResultAndPersists() {
        when(userRepository.findByEmail("u@uniform.test")).thenReturn(Optional.of(user));
        when(tryOnJobRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.of(processingJob()));
        when(provider.poll("https://resp/1"))
                .thenReturn(new VirtualTryOnProvider.PollResult(TryOnStatus.SUCCEEDED, "https://fal/result.png", null));
        when(cloudinaryService.uploadImageFromUrl(eq("https://fal/result.png"), any(), any()))
                .thenReturn(CloudinaryUploadResult.builder()
                        .secureUrl("https://cdn/stored.png").publicId("pid").build());
        when(tryOnJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TryOnJobDto dto = service.getJob("u@uniform.test", 5L);

        assertThat(dto.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(dto.getResultImageUrl()).isEqualTo("https://cdn/stored.png");
        verify(cloudinaryService).uploadImageFromUrl(eq("https://fal/result.png"), any(), any());
        verify(tryOnJobRepository).save(any());
    }

    @Test
    void getJob_stillProcessing_doesNotUploadOrFail() {
        when(userRepository.findByEmail("u@uniform.test")).thenReturn(Optional.of(user));
        when(tryOnJobRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.of(processingJob()));
        when(provider.poll("https://resp/1"))
                .thenReturn(new VirtualTryOnProvider.PollResult(TryOnStatus.PROCESSING, null, null));

        TryOnJobDto dto = service.getJob("u@uniform.test", 5L);

        assertThat(dto.getStatus()).isEqualTo("PROCESSING");
        verify(cloudinaryService, never()).uploadImageFromUrl(any(), any(), any());
        verify(tryOnJobRepository, never()).save(any());
    }

    @Test
    void getJob_alreadySucceeded_doesNotPoll() {
        TryOnJob done = TryOnJob.builder()
                .product(product).status(TryOnStatus.SUCCEEDED)
                .userImageUrl("https://cdn/user.jpg").garmentImageUrl("https://cdn/shirt.jpg")
                .resultImageUrl("https://cdn/result.png").build();
        done.setId(5L);
        when(userRepository.findByEmail("u@uniform.test")).thenReturn(Optional.of(user));
        when(tryOnJobRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.of(done));

        TryOnJobDto dto = service.getJob("u@uniform.test", 5L);

        assertThat(dto.getStatus()).isEqualTo("SUCCEEDED");
        verify(provider, never()).poll(any());
    }

    @Test
    void getJob_notFound_throws() {
        when(userRepository.findByEmail("u@uniform.test")).thenReturn(Optional.of(user));
        when(tryOnJobRepository.findByIdAndUserId(404L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJob("u@uniform.test", 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private TryOnJob processingJob() {
        TryOnJob job = TryOnJob.builder()
                .product(product)
                .status(TryOnStatus.PROCESSING)
                .provider("FAL_FASHN")
                .providerResponseUrl("https://resp/1")
                .userImageUrl("https://cdn/user.jpg")
                .garmentImageUrl("https://cdn/shirt.jpg")
                .garmentPhotoType("auto")
                .build();
        job.setId(5L);
        return job;
    }

    private static ProductImage image(String url) {
        return ProductImage.builder().url(url).isPrimary(true).sortOrder(0).build();
    }
}
