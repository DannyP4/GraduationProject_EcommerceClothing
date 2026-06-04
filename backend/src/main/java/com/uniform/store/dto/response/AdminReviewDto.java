package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminReviewDto {
    private Long id;
    private Integer rating;
    private String title;
    private String body;
    private ReviewStatus status;
    private Boolean verifiedPurchase;
    private Integer helpfulCount;
    private Long productId;
    private String productName;
    private String productSlug;
    private Long userId;
    private String userEmail;
    private String userName;
    private List<String> images;
    private Instant createdAt;
}
