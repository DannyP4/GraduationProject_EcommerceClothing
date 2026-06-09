package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandSummaryDto {
    private Long id;
    private String slug;
    private String name;
    private String description;
    private String logoUrl;
    private String websiteUrl;
    private long productCount;
    private long soldCount;
    private Double averageRating;
    private long reviewCount;
}
