package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudinaryUploadResult {

    private final String publicId;
    private final String secureUrl;
    private final Integer width;
    private final Integer height;
    private final String format;
    private final Long bytes;
}
