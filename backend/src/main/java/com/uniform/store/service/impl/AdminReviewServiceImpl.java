package com.uniform.store.service.impl;

import com.uniform.store.dto.response.AdminReviewDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.entity.Review;
import com.uniform.store.entity.ReviewImage;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.mapper.ReviewMapper;
import com.uniform.store.repository.ReviewImageRepository;
import com.uniform.store.repository.ReviewRepository;
import com.uniform.store.service.AdminReviewService;
import com.uniform.store.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewMapper reviewMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public PageResponse<AdminReviewDto> listReviews(ReviewStatus status, String search, Pageable pageable) {
        String term = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Review> page = reviewRepository.adminSearch(status, term, pageable);
        return PageResponse.from(page, reviewMapper.toAdminList(page.getContent()));
    }

    @Override
    public AdminReviewDto getById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", String.valueOf(reviewId)));
        return reviewMapper.toAdminDto(review);
    }

    @Override
    @Transactional
    public AdminReviewDto approve(Long reviewId) {
        return setStatus(reviewId, ReviewStatus.APPROVED);
    }

    @Override
    @Transactional
    public AdminReviewDto reject(Long reviewId) {
        return setStatus(reviewId, ReviewStatus.REJECTED);
    }

    @Override
    @Transactional
    public void delete(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", String.valueOf(reviewId)));
        List<ReviewImage> images = reviewImageRepository.findByReviewIdOrderBySortOrderAscIdAsc(reviewId);
        reviewRepository.delete(review);
        images.forEach(img -> safeDeleteCloudinary(img.getPublicId()));
    }

    private AdminReviewDto setStatus(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", String.valueOf(reviewId)));
        review.setStatus(status);
        reviewRepository.save(review);
        return reviewMapper.toAdminDto(review);
    }

    private void safeDeleteCloudinary(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinaryService.deleteByPublicId(publicId);
        } catch (Exception e) {
            log.warn("Cloudinary cleanup failed for review image {}: {}", publicId, e.getMessage());
        }
    }
}
