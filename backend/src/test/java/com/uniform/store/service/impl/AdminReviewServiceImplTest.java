package com.uniform.store.service.impl;

import com.uniform.store.dto.response.AdminReviewDto;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.Review;
import com.uniform.store.entity.ReviewImage;
import com.uniform.store.entity.User;
import com.uniform.store.enums.ReviewStatus;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.mapper.ReviewMapper;
import com.uniform.store.repository.ReviewImageRepository;
import com.uniform.store.repository.ReviewRepository;
import com.uniform.store.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReviewServiceImplTest {

    @Mock ReviewRepository reviewRepository;
    @Mock ReviewImageRepository reviewImageRepository;
    @Mock ReviewMapper reviewMapper;
    @Mock CloudinaryService cloudinaryService;

    AdminReviewServiceImpl service;

    Review review;

    @BeforeEach
    void setUp() {
        service = new AdminReviewServiceImpl(reviewRepository, reviewImageRepository, reviewMapper, cloudinaryService);

        User author = User.builder().email("buyer@uniform.test").fullName("Buyer").status(UserStatus.ACTIVE).build();
        author.setId(2L);
        Product product = Product.builder().name("Tee").slug("tee").basePrice(new BigDecimal("100000")).build();
        product.setId(10L);
        review = Review.builder().user(author).product(product).rating(5)
                .status(ReviewStatus.PENDING).helpfulCount(0).build();
        review.setId(50L);
    }

    @Test
    void approve_setsApproved() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));
        when(reviewMapper.toAdminDto(review)).thenReturn(AdminReviewDto.builder().id(50L).status(ReviewStatus.APPROVED).build());

        AdminReviewDto dto = service.approve(50L);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(dto.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        verify(reviewRepository).save(review);
    }

    @Test
    void reject_setsRejected() {
        review.setStatus(ReviewStatus.APPROVED);
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));
        when(reviewMapper.toAdminDto(review)).thenReturn(AdminReviewDto.builder().id(50L).status(ReviewStatus.REJECTED).build());

        AdminReviewDto dto = service.reject(50L);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(dto.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        verify(reviewRepository).save(review);
    }

    @Test
    void approve_notFound_throws() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_cleansCloudinaryAndDeletes() {
        ReviewImage img = ReviewImage.builder().review(review).url("https://x/y.jpg").publicId("uniform/reviews/y").build();
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAscIdAsc(50L)).thenReturn(List.of(img));

        service.delete(50L);

        verify(reviewRepository).delete(review);
        verify(cloudinaryService).deleteByPublicId("uniform/reviews/y");
    }
}
