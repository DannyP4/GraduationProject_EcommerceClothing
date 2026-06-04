package com.uniform.store.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CreateReviewRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 5000)
    private String body;

    @Size(max = 4, message = "A review can have at most 4 images")
    private List<@Valid ReviewImageInput> images;
}
