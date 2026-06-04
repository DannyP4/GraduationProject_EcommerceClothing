package com.uniform.store.service;

import com.uniform.store.dto.request.CreateReviewRequest;
import com.uniform.store.dto.request.UpdateReviewRequest;
import com.uniform.store.dto.response.HelpfulResultDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.ReviewEligibilityDto;
import com.uniform.store.dto.response.ReviewResponseDto;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    PageResponse<ReviewResponseDto> listProductReviews(String idOrSlug, Pageable pageable, String currentUserEmail);

    ReviewResponseDto createReview(String email, CreateReviewRequest req);

    ReviewResponseDto updateReview(String email, Long reviewId, UpdateReviewRequest req);

    void deleteReview(String email, Long reviewId);

    ReviewEligibilityDto checkEligibility(String email, Long productId);

    HelpfulResultDto setHelpful(String email, Long reviewId, boolean helpful);
}
