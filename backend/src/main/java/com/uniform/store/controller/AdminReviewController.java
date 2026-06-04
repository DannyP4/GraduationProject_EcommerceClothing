package com.uniform.store.controller;

import com.uniform.store.dto.response.AdminReviewDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.service.AdminReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "Admin Reviews")
@SecurityRequirement(name = "bearerAuth")
public class AdminReviewController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminReviewService adminReviewService;

    @GetMapping
    @Operation(summary = "List reviews with optional status / search filter")
    public ApiResponse<PageResponse<AdminReviewDto>> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(adminReviewService.listReviews(status, search, pageable));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a review (make it public)")
    public ApiResponse<AdminReviewDto> approve(@PathVariable Long id) {
        return ApiResponse.ok("Review approved", adminReviewService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a review (hide it from the storefront)")
    public ApiResponse<AdminReviewDto> reject(@PathVariable Long id) {
        return ApiResponse.ok("Review rejected", adminReviewService.reject(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a review permanently")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminReviewService.delete(id);
        return ApiResponse.ok("Review deleted", null);
    }
}
