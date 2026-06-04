package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ReviewResponseDto {
    private Long id;
    private Integer rating;
    private String title;
    private String body;
    private String authorName;
    private Boolean verifiedPurchase;
    private Integer helpfulCount;
    private Boolean helpfulByMe;
    private Boolean mine;
    private String variantColor;
    private String variantSize;
    private String variantColorHex;
    private List<String> images;
    private Instant createdAt;
}
