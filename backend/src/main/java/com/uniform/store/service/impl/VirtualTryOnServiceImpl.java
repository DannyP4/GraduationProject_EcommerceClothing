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
import com.uniform.store.service.VirtualTryOnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirtualTryOnServiceImpl implements VirtualTryOnService {

    private static final String RESULT_FOLDER = "uniform/tryon";
    private static final String DEFAULT_PHOTO_TYPE = "auto";

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final TryOnJobRepository tryOnJobRepository;
    private final VirtualTryOnProvider provider;
    private final CloudinaryService cloudinaryService;

    @Override
    public TryOnJobDto createJob(String userEmail, TryOnCreateRequest req) {
        if (!provider.isEnabled()) {
            throw new TryOnException("Virtual try-on is not available right now");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", req.getProductId()));
        if (Boolean.FALSE.equals(product.getIsActive()) || product.getDeletedAt() != null) {
            throw new BadRequestException("Product is not available for try-on");
        }

        String garmentUrl = primaryImageUrl(product.getId());
        String photoType = normalizePhotoType(req.getGarmentPhotoType());
        String category = req.getCategory();
        String userImageUrl = req.getUserImageUrl();

        TryOnJob cached = tryOnJobRepository
                .findFirstByUserIdAndProductIdAndUserImageUrlAndGarmentPhotoTypeAndStatusOrderByCreatedAtDesc(
                        user.getId(), product.getId(), userImageUrl, photoType, TryOnStatus.SUCCEEDED)
                .orElse(null);
        if (cached != null) {
            return toDto(cached, true);
        }

        VirtualTryOnProvider.Submission submission =
                provider.submit(userImageUrl, garmentUrl, photoType, category);

        TryOnJob job = tryOnJobRepository.save(TryOnJob.builder()
                .user(user)
                .product(product)
                .status(TryOnStatus.PROCESSING)
                .provider(provider.name())
                .providerRequestId(submission.requestId())
                .providerResponseUrl(submission.responseUrl())
                .userImageUrl(userImageUrl)
                .garmentImageUrl(garmentUrl)
                .garmentPhotoType(photoType)
                .category(category)
                .build());

        return toDto(job, false);
    }

    @Override
    public TryOnJobDto getJob(String userEmail, Long jobId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        TryOnJob job = tryOnJobRepository.findByIdAndUserId(jobId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Try-on job", jobId));

        if (job.getStatus() == TryOnStatus.PROCESSING) {
            refresh(job);
        }
        return toDto(job, false);
    }

    private void refresh(TryOnJob job) {
        VirtualTryOnProvider.PollResult result;
        try {
            result = provider.poll(job.getProviderResponseUrl());
        } catch (TryOnException e) {
            // Transient upstream blip
            log.warn("Try-on poll failed transiently for job {}: {}", job.getId(), e.getMessage());
            return;
        }

        switch (result.status()) {
            case SUCCEEDED -> {
                CloudinaryUploadResult stored = cloudinaryService.uploadImageFromUrl(
                        result.resultImageUrl(), RESULT_FOLDER, "tryon-" + job.getId());
                job.setResultImageUrl(stored.getSecureUrl());
                job.setResultPublicId(stored.getPublicId());
                job.setStatus(TryOnStatus.SUCCEEDED);
                tryOnJobRepository.save(job);
            }
            case FAILED -> {
                job.setStatus(TryOnStatus.FAILED);
                job.setErrorMessage(truncate(result.error()));
                tryOnJobRepository.save(job);
            }
            default -> { }
        }
    }

    private String primaryImageUrl(Long productId) {
        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByIsPrimaryDescSortOrderAsc(productId);
        if (images.isEmpty()) {
            throw new BadRequestException("This product has no image to try on");
        }
        return images.get(0).getUrl();
    }

    private static String normalizePhotoType(String value) {
        return (value == null || value.isBlank()) ? DEFAULT_PHOTO_TYPE : value;
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private TryOnJobDto toDto(TryOnJob job, boolean cached) {
        return TryOnJobDto.builder()
                .id(job.getId())
                .productId(job.getProduct().getId())
                .status(job.getStatus().name())
                .userImageUrl(job.getUserImageUrl())
                .garmentImageUrl(job.getGarmentImageUrl())
                .resultImageUrl(job.getResultImageUrl())
                .errorMessage(job.getErrorMessage())
                .cached(cached)
                .createdAt(job.getCreatedAt())
                .build();
    }
}
