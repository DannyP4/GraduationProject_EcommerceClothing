package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateReviewRequest;
import com.uniform.store.dto.request.ReviewImageInput;
import com.uniform.store.dto.request.UpdateReviewRequest;
import com.uniform.store.dto.response.HelpfulResultDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.ReviewEligibilityDto;
import com.uniform.store.dto.response.ReviewResponseDto;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.Review;
import com.uniform.store.entity.ReviewHelpfulVote;
import com.uniform.store.entity.ReviewImage;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.mapper.ReviewMapper;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ReviewHelpfulVoteRepository;
import com.uniform.store.repository.ReviewImageRepository;
import com.uniform.store.repository.ReviewRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.CloudinaryService;
import com.uniform.store.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private static final OrderStatus PURCHASE_STATUS = OrderStatus.DELIVERED;

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewMapper reviewMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public PageResponse<ReviewResponseDto> listProductReviews(String idOrSlug, Pageable pageable, String currentUserEmail) {
        Product product = resolveProduct(idOrSlug);
        Long currentUserId = resolveUserId(currentUserEmail);
        Page<Review> page = reviewRepository.findByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED, pageable);
        return PageResponse.from(page, reviewMapper.toResponseList(page.getContent(), currentUserId));
    }

    @Override
    @Transactional
    public ReviewResponseDto createReview(String email, CreateReviewRequest req) {
        User user = getUser(email);
        Product product = productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", req.getProductId()));

        OrderItem purchase = orderItemRepository
                .findFirstByOrderUserIdAndVariantProductIdAndOrderStatusOrderByOrderPlacedAtDescIdDesc(
                        user.getId(), product.getId(), PURCHASE_STATUS)
                .orElseThrow(() -> new BadRequestException(
                        "You can only review a product you have purchased and received."));
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), product.getId())) {
            throw new BadRequestException("You have already reviewed this product.");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .variant(purchase.getVariant())
                .order(purchase.getOrder())
                .rating(req.getRating())
                .title(trimToNull(req.getTitle()))
                .body(req.getBody().trim())
                .verifiedPurchase(true)
                .helpfulCount(0)
                .status(ReviewStatus.APPROVED)
                .build();
        review = reviewRepository.save(review);
        saveImages(review, req.getImages());

        return reviewMapper.toResponseDto(review, user.getId());
    }

    @Override
    @Transactional
    public ReviewResponseDto updateReview(String email, Long reviewId, UpdateReviewRequest req) {
        User user = getUser(email);
        Review review = reviewRepository.findByIdAndUserId(reviewId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", String.valueOf(reviewId)));

        review.setRating(req.getRating());
        review.setTitle(trimToNull(req.getTitle()));
        review.setBody(req.getBody().trim());
        reviewRepository.save(review);

        replaceImages(review, req.getImages());

        return reviewMapper.toResponseDto(review, user.getId());
    }

    @Override
    @Transactional
    public void deleteReview(String email, Long reviewId) {
        User user = getUser(email);
        Review review = reviewRepository.findByIdAndUserId(reviewId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", String.valueOf(reviewId)));
        List<ReviewImage> images = reviewImageRepository.findByReviewIdOrderBySortOrderAscIdAsc(reviewId);
        reviewRepository.delete(review);
        images.forEach(img -> safeDeleteCloudinary(img.getPublicId()));
    }

    @Override
    public ReviewEligibilityDto checkEligibility(String email, Long productId) {
        User user = getUser(email);
        Product product = productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        Optional<Review> existing = reviewRepository.findFirstByUserIdAndProductId(user.getId(), product.getId());
        if (existing.isPresent()) {
            return ReviewEligibilityDto.builder()
                    .canReview(false)
                    .reason("ALREADY_REVIEWED")
                    .existingReviewId(existing.get().getId())
                    .build();
        }
        boolean purchased = orderItemRepository.existsByOrderUserIdAndVariantProductIdAndOrderStatus(
                user.getId(), product.getId(), PURCHASE_STATUS);
        if (!purchased) {
            return ReviewEligibilityDto.builder().canReview(false).reason("NOT_PURCHASED").build();
        }
        return ReviewEligibilityDto.builder().canReview(true).build();
    }

    @Override
    @Transactional
    public HelpfulResultDto setHelpful(String email, Long reviewId, boolean helpful) {
        User user = getUser(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", String.valueOf(reviewId)));
        if (review.getStatus() != ReviewStatus.APPROVED) {
            throw new ResourceNotFoundException("Review", "id", String.valueOf(reviewId));
        }
        if (review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You cannot mark your own review as helpful.");
        }

        Optional<ReviewHelpfulVote> existing = helpfulVoteRepository.findByReviewIdAndUserId(reviewId, user.getId());
        if (helpful && existing.isEmpty()) {
            helpfulVoteRepository.save(ReviewHelpfulVote.builder().review(review).user(user).build());
            review.setHelpfulCount(review.getHelpfulCount() + 1);
            reviewRepository.save(review);
        } else if (!helpful && existing.isPresent()) {
            helpfulVoteRepository.delete(existing.get());
            review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
            reviewRepository.save(review);
        }
        return HelpfulResultDto.builder().helpfulCount(review.getHelpfulCount()).voted(helpful).build();
    }

    private Product resolveProduct(String idOrSlug) {
        Optional<Product> opt;
        try {
            opt = productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(Long.parseLong(idOrSlug));
        } catch (NumberFormatException e) {
            opt = productRepository.findBySlugAndIsActiveTrueAndDeletedAtIsNull(idOrSlug);
        }
        return opt.orElseThrow(() -> new ResourceNotFoundException("Product", "idOrSlug", idOrSlug));
    }

    private void saveImages(Review review, List<ReviewImageInput> inputs) {
        if (inputs == null || inputs.isEmpty()) return;
        int order = 0;
        for (ReviewImageInput in : inputs) {
            reviewImageRepository.save(ReviewImage.builder()
                    .review(review)
                    .url(in.getUrl().trim())
                    .publicId(trimToNull(in.getPublicId()))
                    .sortOrder(order++)
                    .build());
        }
    }

    private void replaceImages(Review review, List<ReviewImageInput> inputs) {
        List<ReviewImage> existing = reviewImageRepository.findByReviewIdOrderBySortOrderAscIdAsc(review.getId());
        List<ReviewImageInput> in = inputs == null ? List.of() : inputs;
        Set<String> newUrls = in.stream().map(i -> i.getUrl().trim()).collect(Collectors.toSet());
        Map<String, ReviewImage> existingByUrl = existing.stream()
                .collect(Collectors.toMap(ReviewImage::getUrl, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        for (ReviewImage old : existing) {
            if (!newUrls.contains(old.getUrl())) {
                reviewImageRepository.delete(old);
                safeDeleteCloudinary(old.getPublicId());
            }
        }
        int order = 0;
        for (ReviewImageInput i : in) {
            String url = i.getUrl().trim();
            ReviewImage row = existingByUrl.get(url);
            if (row != null) {
                row.setSortOrder(order);
                reviewImageRepository.save(row);
            } else {
                reviewImageRepository.save(ReviewImage.builder()
                        .review(review)
                        .url(url)
                        .publicId(trimToNull(i.getPublicId()))
                        .sortOrder(order)
                        .build());
            }
            order++;
        }
    }

    private void safeDeleteCloudinary(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinaryService.deleteByPublicId(publicId);
        } catch (Exception e) {
            log.warn("Cloudinary cleanup failed for review image {}: {}", publicId, e.getMessage());
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Long resolveUserId(String email) {
        if (email == null || email.isBlank()) return null;
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
