package com.uniform.store.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.uniform.store.dto.response.CloudinarySignatureDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryServiceImplTest {

    private static final String CLOUD_NAME = "test-cloud";
    private static final String API_KEY = "111122223333444";
    private static final String API_SECRET = "fake-secret-fake-secret-fake";
    private static final String DEFAULT_FOLDER = "uniform/products";

    private CloudinaryServiceImpl service;
    private Cloudinary cloudinary;

    @BeforeEach
    void setUp() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", CLOUD_NAME,
                "api_key", API_KEY,
                "api_secret", API_SECRET,
                "secure", true));
        service = new CloudinaryServiceImpl(cloudinary);
        ReflectionTestUtils.setField(service, "uploadFolder", DEFAULT_FOLDER);
    }

    @Test
    void generateSignedUploadParams_returnsCloudNameApiKeyAndRecentTimestamp() {
        long before = Instant.now().getEpochSecond();
        CloudinarySignatureDto dto = service.generateSignedUploadParams(null, "navy-tee");
        long after = Instant.now().getEpochSecond();

        assertThat(dto.getCloudName()).isEqualTo(CLOUD_NAME);
        assertThat(dto.getApiKey()).isEqualTo(API_KEY);
        assertThat(dto.getTimestamp()).isBetween(before, after);
        assertThat(dto.getFolder()).isEqualTo(DEFAULT_FOLDER);
        assertThat(dto.getPublicId()).startsWith("navy-tee-");
        assertThat(dto.getPublicId().length()).isEqualTo("navy-tee-".length() + 8);
        assertThat(dto.getSignature()).hasSize(40).matches("[0-9a-f]+");
    }

    @Test
    void generateSignedUploadParams_signatureMatchesIndependentlyComputedSha1() {
        CloudinarySignatureDto dto = service.generateSignedUploadParams("uniform/custom", "hero");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("folder", dto.getFolder());
        params.put("public_id", dto.getPublicId());
        params.put("timestamp", dto.getTimestamp());
        String expected = cloudinary.apiSignRequest(params, API_SECRET, 1);

        assertThat(dto.getSignature()).isEqualTo(expected);
    }

    @Test
    void generateSignedUploadParams_blankFolderFallsBackToDefault() {
        CloudinarySignatureDto dto = service.generateSignedUploadParams("   ", null);
        assertThat(dto.getFolder()).isEqualTo(DEFAULT_FOLDER);
        assertThat(dto.getPublicId()).startsWith("upload-");
    }

    @Test
    void generateSignedUploadParams_sanitizesFilenameHint() {
        CloudinarySignatureDto dto = service.generateSignedUploadParams(null, "Áo Khoác Đông 2024!");
        assertThat(dto.getPublicId()).matches("[a-z0-9-]+");
    }
}
