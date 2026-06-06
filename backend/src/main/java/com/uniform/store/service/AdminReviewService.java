package com.uniform.store.service;

import com.uniform.store.dto.response.AdminReviewDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.enums.ReviewStatus;
import org.springframework.data.domain.Pageable;

public interface AdminReviewService {

    PageResponse<AdminReviewDto> listReviews(ReviewStatus status, String search, Pageable pageable);

    AdminReviewDto getById(Long reviewId);

    AdminReviewDto approve(Long reviewId);

    AdminReviewDto reject(Long reviewId);

    void delete(Long reviewId);
}
