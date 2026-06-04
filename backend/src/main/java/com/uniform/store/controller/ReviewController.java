package com.uniform.store.controller;

import com.uniform.store.dto.request.CreateReviewRequest;
import com.uniform.store.dto.request.UpdateReviewRequest;
import com.uniform.store.dto.request.UploadSignatureRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.CloudinarySignatureDto;
import com.uniform.store.dto.response.HelpfulResultDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.ReviewEligibilityDto;
import com.uniform.store.dto.response.ReviewResponseDto;
import com.uniform.store.service.CloudinaryService;
import com.uniform.store.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ReviewController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String REVIEW_UPLOAD_FOLDER = "uniform/reviews";

    private final ReviewService reviewService;
    private final CloudinaryService cloudinaryService;

    @GetMapping("/products/{idOrSlug}/reviews")
    @Operation(summary = "List approved reviews for a product")
    public ApiResponse<PageResponse<ReviewResponseDto>> listForProduct(
            @PathVariable String idOrSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort,
            Authentication auth) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, sortFor(sort));
        return ApiResponse.ok(reviewService.listProductReviews(idOrSlug, pageable, currentEmail(auth)));
    }

    @PostMapping("/reviews")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a review (verified purchase required)")
    public ApiResponse<ReviewResponseDto> create(Authentication auth, @Valid @RequestBody CreateReviewRequest req) {
        return ApiResponse.ok("Review submitted", reviewService.createReview(auth.getName(), req));
    }

    @PutMapping("/reviews/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update your own review")
    public ApiResponse<ReviewResponseDto> update(Authentication auth, @PathVariable Long id,
                                                 @Valid @RequestBody UpdateReviewRequest req) {
        return ApiResponse.ok("Review updated", reviewService.updateReview(auth.getName(), id, req));
    }

    @DeleteMapping("/reviews/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete your own review")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        reviewService.deleteReview(auth.getName(), id);
        return ApiResponse.ok("Review deleted", null);
    }

    @GetMapping("/reviews/eligibility")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Whether the current user can review a product")
    public ApiResponse<ReviewEligibilityDto> eligibility(Authentication auth, @RequestParam Long productId) {
        return ApiResponse.ok(reviewService.checkEligibility(auth.getName(), productId));
    }

    @PostMapping("/reviews/{id}/helpful")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark a review as helpful")
    public ApiResponse<HelpfulResultDto> markHelpful(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok(reviewService.setHelpful(auth.getName(), id, true));
    }

    @DeleteMapping("/reviews/{id}/helpful")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove your helpful vote from a review")
    public ApiResponse<HelpfulResultDto> unmarkHelpful(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok(reviewService.setHelpful(auth.getName(), id, false));
    }

    @PostMapping("/reviews/upload-signature")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cloudinary signed-upload payload for review images")
    public ApiResponse<CloudinarySignatureDto> sign(@Valid @RequestBody(required = false) UploadSignatureRequest req) {
        String hint = req == null ? null : req.getFilenameHint();
        return ApiResponse.ok(cloudinaryService.generateSignedUploadParams(REVIEW_UPLOAD_FOLDER, hint));
    }

    private Sort sortFor(String sort) {
        if ("helpful".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Order.desc("helpfulCount"), Sort.Order.desc("createdAt"));
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private String currentEmail(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }
}
