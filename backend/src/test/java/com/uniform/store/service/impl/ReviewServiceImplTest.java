package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateReviewRequest;
import com.uniform.store.dto.request.UpdateReviewRequest;
import com.uniform.store.dto.response.HelpfulResultDto;
import com.uniform.store.dto.response.ReviewEligibilityDto;
import com.uniform.store.dto.response.ReviewResponseDto;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Review;
import com.uniform.store.entity.ReviewHelpfulVote;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.enums.UserStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock ReviewRepository reviewRepository;
    @Mock ReviewImageRepository reviewImageRepository;
    @Mock ReviewHelpfulVoteRepository helpfulVoteRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ReviewMapper reviewMapper;
    @Mock CloudinaryService cloudinaryService;

    ReviewServiceImpl service;

    User buyer;
    Product product;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository, reviewImageRepository, helpfulVoteRepository,
                productRepository, userRepository, orderItemRepository,
                reviewMapper, cloudinaryService);

        buyer = User.builder().email("buyer@uniform.test").fullName("Buyer One").status(UserStatus.ACTIVE).build();
        buyer.setId(2L);

        product = Product.builder().name("Tee").slug("tee").basePrice(new BigDecimal("100000"))
                .currency("VND").isActive(true).build();
        product.setId(10L);
    }

    private CreateReviewRequest createRequest(int rating, String body) {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setProductId(10L);
        req.setRating(rating);
        req.setBody(body);
        return req;
    }

    private OrderItem deliveredItem() {
        ProductVariant variant = ProductVariant.builder()
                .product(product).sku("SKU-1").size("M").color("Black").colorHex("#000000").build();
        variant.setId(20L);
        return OrderItem.builder().order(Order.builder().build()).variant(variant).build();
    }

    @Test
    void createReview_withoutDeliveredPurchase_throws() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(10L)).thenReturn(Optional.of(product));
        when(orderItemRepository.findFirstByOrderUserIdAndVariantProductIdAndOrderStatusOrderByOrderPlacedAtDescIdDesc(
                2L, 10L, OrderStatus.DELIVERED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReview("buyer@uniform.test", createRequest(5, "Great")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("purchased");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_alreadyReviewed_throws() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(10L)).thenReturn(Optional.of(product));
        when(orderItemRepository.findFirstByOrderUserIdAndVariantProductIdAndOrderStatusOrderByOrderPlacedAtDescIdDesc(
                2L, 10L, OrderStatus.DELIVERED)).thenReturn(Optional.of(deliveredItem()));
        when(reviewRepository.existsByUserIdAndProductId(2L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.createReview("buyer@uniform.test", createRequest(5, "Great")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already reviewed");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_valid_savesApprovedAndVerified() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(10L)).thenReturn(Optional.of(product));
        OrderItem purchase = deliveredItem();
        when(orderItemRepository.findFirstByOrderUserIdAndVariantProductIdAndOrderStatusOrderByOrderPlacedAtDescIdDesc(
                2L, 10L, OrderStatus.DELIVERED)).thenReturn(Optional.of(purchase));
        when(reviewRepository.existsByUserIdAndProductId(2L, 10L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });
        when(reviewMapper.toResponseDto(any(Review.class), eq(2L)))
                .thenReturn(ReviewResponseDto.builder().id(99L).build());

        ReviewResponseDto dto = service.createReview("buyer@uniform.test", createRequest(4, "Nice fit"));

        assertThat(dto.getId()).isEqualTo(99L);

        ArgumentCaptor<Review> cap = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(cap.capture());
        Review saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(saved.getVerifiedPurchase()).isTrue();
        assertThat(saved.getRating()).isEqualTo(4);
        assertThat(saved.getUser()).isEqualTo(buyer);
        assertThat(saved.getProduct()).isEqualTo(product);
        assertThat(saved.getVariant()).isEqualTo(purchase.getVariant());
        assertThat(saved.getOrder()).isEqualTo(purchase.getOrder());
    }

    @Test
    void setHelpful_ownReview_throws() {
        Review own = Review.builder().user(buyer).product(product).rating(5)
                .status(ReviewStatus.APPROVED).helpfulCount(0).build();
        own.setId(50L);
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(own));

        assertThatThrownBy(() -> service.setHelpful("buyer@uniform.test", 50L, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("your own");

        verify(helpfulVoteRepository, never()).save(any());
    }

    @Test
    void setHelpful_vote_incrementsAndSavesVote() {
        User voter = User.builder().email("voter@uniform.test").fullName("Voter").status(UserStatus.ACTIVE).build();
        voter.setId(3L);
        Review r = Review.builder().user(buyer).product(product).rating(5)
                .status(ReviewStatus.APPROVED).helpfulCount(2).build();
        r.setId(50L);
        when(userRepository.findByEmail("voter@uniform.test")).thenReturn(Optional.of(voter));
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(r));
        when(helpfulVoteRepository.findByReviewIdAndUserId(50L, 3L)).thenReturn(Optional.empty());

        HelpfulResultDto res = service.setHelpful("voter@uniform.test", 50L, true);

        assertThat(res.getVoted()).isTrue();
        assertThat(res.getHelpfulCount()).isEqualTo(3);
        assertThat(r.getHelpfulCount()).isEqualTo(3);
        verify(helpfulVoteRepository).save(any(ReviewHelpfulVote.class));
        verify(reviewRepository).save(r);
    }

    @Test
    void setHelpful_voteTwice_isIdempotent() {
        User voter = User.builder().email("voter@uniform.test").fullName("Voter").status(UserStatus.ACTIVE).build();
        voter.setId(3L);
        Review r = Review.builder().user(buyer).product(product).rating(5)
                .status(ReviewStatus.APPROVED).helpfulCount(2).build();
        r.setId(50L);
        ReviewHelpfulVote existing = ReviewHelpfulVote.builder().review(r).user(voter).build();
        when(userRepository.findByEmail("voter@uniform.test")).thenReturn(Optional.of(voter));
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(r));
        when(helpfulVoteRepository.findByReviewIdAndUserId(50L, 3L)).thenReturn(Optional.of(existing));

        HelpfulResultDto res = service.setHelpful("voter@uniform.test", 50L, true);

        assertThat(res.getHelpfulCount()).isEqualTo(2);
        assertThat(r.getHelpfulCount()).isEqualTo(2);
        verify(helpfulVoteRepository, never()).save(any());
    }

    @Test
    void setHelpful_unvote_decrements() {
        User voter = User.builder().email("voter@uniform.test").fullName("Voter").status(UserStatus.ACTIVE).build();
        voter.setId(3L);
        Review r = Review.builder().user(buyer).product(product).rating(5)
                .status(ReviewStatus.APPROVED).helpfulCount(2).build();
        r.setId(50L);
        ReviewHelpfulVote existing = ReviewHelpfulVote.builder().review(r).user(voter).build();
        when(userRepository.findByEmail("voter@uniform.test")).thenReturn(Optional.of(voter));
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(r));
        when(helpfulVoteRepository.findByReviewIdAndUserId(50L, 3L)).thenReturn(Optional.of(existing));

        HelpfulResultDto res = service.setHelpful("voter@uniform.test", 50L, false);

        assertThat(res.getVoted()).isFalse();
        assertThat(res.getHelpfulCount()).isEqualTo(1);
        verify(helpfulVoteRepository).delete(existing);
    }

    @Test
    void setHelpful_nonApprovedReview_throwsNotFound() {
        User voter = User.builder().email("voter@uniform.test").fullName("Voter").status(UserStatus.ACTIVE).build();
        voter.setId(3L);
        Review pending = Review.builder().user(buyer).product(product).rating(5)
                .status(ReviewStatus.PENDING).helpfulCount(0).build();
        pending.setId(50L);
        when(userRepository.findByEmail("voter@uniform.test")).thenReturn(Optional.of(voter));
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.setHelpful("voter@uniform.test", 50L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReview_notOwner_throwsNotFound() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(reviewRepository.findByIdAndUserId(50L, 2L)).thenReturn(Optional.empty());

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setRating(3);
        req.setBody("changed");

        assertThatThrownBy(() -> service.updateReview("buyer@uniform.test", 50L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void checkEligibility_alreadyReviewed() {
        Review existing = Review.builder().user(buyer).product(product).rating(5)
                .status(ReviewStatus.APPROVED).build();
        existing.setId(77L);
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(10L)).thenReturn(Optional.of(product));
        when(reviewRepository.findFirstByUserIdAndProductId(2L, 10L)).thenReturn(Optional.of(existing));

        ReviewEligibilityDto dto = service.checkEligibility("buyer@uniform.test", 10L);

        assertThat(dto.getCanReview()).isFalse();
        assertThat(dto.getReason()).isEqualTo("ALREADY_REVIEWED");
        assertThat(dto.getExistingReviewId()).isEqualTo(77L);
    }

    @Test
    void checkEligibility_notPurchased() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(10L)).thenReturn(Optional.of(product));
        when(reviewRepository.findFirstByUserIdAndProductId(2L, 10L)).thenReturn(Optional.empty());
        when(orderItemRepository.existsByOrderUserIdAndVariantProductIdAndOrderStatus(2L, 10L, OrderStatus.DELIVERED))
                .thenReturn(false);

        ReviewEligibilityDto dto = service.checkEligibility("buyer@uniform.test", 10L);

        assertThat(dto.getCanReview()).isFalse();
        assertThat(dto.getReason()).isEqualTo("NOT_PURCHASED");
    }

    @Test
    void checkEligibility_canReview() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(10L)).thenReturn(Optional.of(product));
        when(reviewRepository.findFirstByUserIdAndProductId(2L, 10L)).thenReturn(Optional.empty());
        when(orderItemRepository.existsByOrderUserIdAndVariantProductIdAndOrderStatus(2L, 10L, OrderStatus.DELIVERED))
                .thenReturn(true);

        ReviewEligibilityDto dto = service.checkEligibility("buyer@uniform.test", 10L);

        assertThat(dto.getCanReview()).isTrue();
        assertThat(dto.getReason()).isNull();
    }
}
