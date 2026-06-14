package com.uniform.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TryOnCreateRequest {

    @NotNull
    private Long productId;

    @NotBlank
    @Size(max = 500)
    private String userImageUrl;

    @Pattern(regexp = "auto|model|flat-lay", message = "garmentPhotoType must be auto, model or flat-lay")
    private String garmentPhotoType;

    @Pattern(regexp = "auto|tops|bottoms|one-pieces", message = "category must be auto, tops, bottoms or one-pieces")
    private String category;
}
