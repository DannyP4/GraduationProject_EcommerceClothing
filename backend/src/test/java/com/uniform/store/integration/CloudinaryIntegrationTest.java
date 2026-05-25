package com.uniform.store.integration;

import com.uniform.store.dto.response.CloudinaryUploadResult;
import com.uniform.store.service.CloudinaryService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryIntegrationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryIntegrationTest.class);

    private static final String ONE_PIXEL_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

    @Autowired
    private CloudinaryService cloudinaryService;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Test
    void uploadImage_returnsSecureUrlAndPublicId() {
        Assumptions.assumeTrue(cloudName != null && !cloudName.isBlank(),
                "Cloudinary not configured in active profile; skipping integration test");

        byte[] pngBytes = Base64.getDecoder().decode(ONE_PIXEL_PNG_BASE64);

        CloudinaryUploadResult result = cloudinaryService.uploadImage(pngBytes, "smoke-test");

        assertThat(result).isNotNull();
        assertThat(result.getPublicId()).isNotBlank();
        assertThat(result.getSecureUrl())
                .startsWith("https://res.cloudinary.com/" + cloudName + "/");
        assertThat(result.getWidth()).isEqualTo(1);
        assertThat(result.getHeight()).isEqualTo(1);

        log.info("CLOUDINARY VERIFY -> publicId={} secureUrl={}", result.getPublicId(), result.getSecureUrl());

        cloudinaryService.deleteByPublicId(result.getPublicId());
    }
}
