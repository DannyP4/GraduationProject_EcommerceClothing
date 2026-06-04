package com.uniform.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReviewImageInput {

    @NotBlank
    @Size(max = 500)
    private String url;

    @Size(max = 255)
    private String publicId;
}
