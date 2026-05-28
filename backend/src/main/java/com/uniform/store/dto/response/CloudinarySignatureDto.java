package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudinarySignatureDto {

    private final String cloudName;
    private final String apiKey;
    private final long timestamp;
    private final String signature;
    private final String folder;
    private final String publicId;
}
