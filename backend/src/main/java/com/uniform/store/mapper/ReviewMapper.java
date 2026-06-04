package com.uniform.store.mapper;

import com.uniform.store.dto.response.AdminReviewDto;
import com.uniform.store.dto.response.ReviewResponseDto;
import com.uniform.store.entity.Review;
import com.uniform.store.entity.ReviewImage;
import com.uniform.store.repository.ReviewHelpfulVoteRepository;
import com.uniform.store.repository.ReviewImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReviewMapper {

    private final ReviewImageRepository reviewImageRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;

    public List<ReviewResponseDto> toResponseList(List<Review> reviews, Long currentUserId) {
        if (reviews.isEmpty()) return List.of();
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        Map<Long, List<String>> imagesByReview = imagesByReview(ids);
        Set<Long> votedIds = (currentUserId == null)
                ? Set.of()
                : new HashSet<>(helpfulVoteRepository.findVotedReviewIds(ids, currentUserId));
        return reviews.stream()
                .map(r -> buildResponse(r, currentUserId, votedIds, imagesByReview.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    public ReviewResponseDto toResponseDto(Review r, Long currentUserId) {
        List<String> images = reviewImageRepository.findByReviewIdOrderBySortOrderAscIdAsc(r.getId())
                .stream().map(ReviewImage::getUrl).toList();
        Set<Long> voted = (currentUserId != null
                && helpfulVoteRepository.existsByReviewIdAndUserId(r.getId(), currentUserId))
                ? Set.of(r.getId()) : Set.of();
        return buildResponse(r, currentUserId, voted, images);
    }

    public List<AdminReviewDto> toAdminList(List<Review> reviews) {
        if (reviews.isEmpty()) return List.of();
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        Map<Long, List<String>> imagesByReview = imagesByReview(ids);
        return reviews.stream()
                .map(r -> buildAdmin(r, imagesByReview.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    public AdminReviewDto toAdminDto(Review r) {
        List<String> images = reviewImageRepository.findByReviewIdOrderBySortOrderAscIdAsc(r.getId())
                .stream().map(ReviewImage::getUrl).toList();
        return buildAdmin(r, images);
    }

    private ReviewResponseDto buildResponse(Review r, Long currentUserId, Set<Long> votedIds, List<String> images) {
        boolean mine = currentUserId != null && r.getUser().getId().equals(currentUserId);
        ReviewResponseDto.ReviewResponseDtoBuilder b = ReviewResponseDto.builder()
                .id(r.getId())
                .rating(r.getRating())
                .title(r.getTitle())
                .body(r.getBody())
                .authorName(maskName(r.getUser().getFullName()))
                .verifiedPurchase(r.getVerifiedPurchase())
                .helpfulCount(r.getHelpfulCount())
                .helpfulByMe(votedIds.contains(r.getId()))
                .mine(mine)
                .images(images)
                .createdAt(r.getCreatedAt());
        if (r.getVariant() != null) {
            b.variantColor(r.getVariant().getColor())
                    .variantSize(r.getVariant().getSize())
                    .variantColorHex(r.getVariant().getColorHex());
        }
        return b.build();
    }

    private AdminReviewDto buildAdmin(Review r, List<String> images) {
        return AdminReviewDto.builder()
                .id(r.getId())
                .rating(r.getRating())
                .title(r.getTitle())
                .body(r.getBody())
                .status(r.getStatus())
                .verifiedPurchase(r.getVerifiedPurchase())
                .helpfulCount(r.getHelpfulCount())
                .productId(r.getProduct().getId())
                .productName(r.getProduct().getName())
                .productSlug(r.getProduct().getSlug())
                .userId(r.getUser().getId())
                .userEmail(r.getUser().getEmail())
                .userName(r.getUser().getFullName())
                .images(images)
                .createdAt(r.getCreatedAt())
                .build();
    }

    private Map<Long, List<String>> imagesByReview(List<Long> reviewIds) {
        Map<Long, List<String>> map = new LinkedHashMap<>();
        for (ReviewImage img : reviewImageRepository.findByReviewIdInOrderBySortOrderAscIdAsc(reviewIds)) {
            map.computeIfAbsent(img.getReview().getId(), k -> new ArrayList<>()).add(img.getUrl());
        }
        return map;
    }

    private String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Anonymous";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(' ').append(Character.toUpperCase(parts[i].charAt(0))).append('.');
        }
        return sb.toString();
    }
}
