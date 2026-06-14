package com.uniform.store.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.uniform.store.dto.response.CloudinarySignatureDto;
import com.uniform.store.dto.response.CloudinaryUploadResult;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.upload-folder:uniform/products}")
    private String uploadFolder;

    @Override
    public CloudinaryUploadResult uploadImage(byte[] bytes, String filenameHint) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("Image bytes are empty");
        }
        String publicId = sanitize(filenameHint) + "-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "folder", uploadFolder,
                    "public_id", publicId,
                    "resource_type", "image",
                    "overwrite", false,
                    "unique_filename", false));
            return CloudinaryUploadResult.builder()
                    .publicId((String) result.get("public_id"))
                    .secureUrl((String) result.get("secure_url"))
                    .width(toInt(result.get("width")))
                    .height(toInt(result.get("height")))
                    .format((String) result.get("format"))
                    .bytes(toLong(result.get("bytes")))
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Cloudinary upload failed: " + e.getMessage());
        }
    }

    @Override
    public CloudinaryUploadResult uploadImageFromUrl(String remoteUrl, String folder, String filenameHint) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            throw new BadRequestException("Remote image url is empty");
        }
        String targetFolder = (folder == null || folder.isBlank()) ? uploadFolder : folder;
        String publicId = sanitize(filenameHint) + "-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(remoteUrl, ObjectUtils.asMap(
                    "folder", targetFolder,
                    "public_id", publicId,
                    "resource_type", "image",
                    "overwrite", false,
                    "unique_filename", false));
            return CloudinaryUploadResult.builder()
                    .publicId((String) result.get("public_id"))
                    .secureUrl((String) result.get("secure_url"))
                    .width(toInt(result.get("width")))
                    .height(toInt(result.get("height")))
                    .format((String) result.get("format"))
                    .bytes(toLong(result.get("bytes")))
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Cloudinary remote upload failed: " + e.getMessage());
        }
    }

    @Override
    public CloudinarySignatureDto generateSignedUploadParams(String folder, String filenameHint) {
        String targetFolder = (folder == null || folder.isBlank()) ? uploadFolder : folder;
        String publicId = sanitize(filenameHint) + "-" + UUID.randomUUID().toString().substring(0, 8);
        long timestamp = Instant.now().getEpochSecond();

        Map<String, Object> paramsToSign = new LinkedHashMap<>();
        paramsToSign.put("folder", targetFolder);
        paramsToSign.put("public_id", publicId);
        paramsToSign.put("timestamp", timestamp);

        String apiSecret = cloudinary.config.apiSecret;
        String signature = cloudinary.apiSignRequest(paramsToSign, apiSecret, 1);

        return CloudinarySignatureDto.builder()
                .cloudName(cloudinary.config.cloudName)
                .apiKey(cloudinary.config.apiKey)
                .timestamp(timestamp)
                .signature(signature)
                .folder(targetFolder)
                .publicId(publicId)
                .build();
    }

    @Override
    public void deleteByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            throw new BadRequestException("Cloudinary delete failed: " + e.getMessage());
        }
    }

    private String sanitize(String hint) {
        if (hint == null || hint.isBlank()) {
            return "upload";
        }
        return hint.toLowerCase().replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
    }

    private Integer toInt(Object v) {
        return v instanceof Number n ? n.intValue() : null;
    }

    private Long toLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }
}
