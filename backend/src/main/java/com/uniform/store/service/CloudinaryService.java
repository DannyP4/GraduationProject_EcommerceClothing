package com.uniform.store.service;

import com.uniform.store.dto.response.CloudinarySignatureDto;
import com.uniform.store.dto.response.CloudinaryUploadResult;

public interface CloudinaryService {

    CloudinaryUploadResult uploadImage(byte[] bytes, String filenameHint);

    CloudinaryUploadResult uploadImageFromUrl(String remoteUrl, String folder, String filenameHint);

    void deleteByPublicId(String publicId);

    CloudinarySignatureDto generateSignedUploadParams(String folder, String filenameHint);
}
