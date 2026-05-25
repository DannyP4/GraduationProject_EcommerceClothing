package com.uniform.store.service;

import com.uniform.store.dto.response.CloudinaryUploadResult;

public interface CloudinaryService {

    CloudinaryUploadResult uploadImage(byte[] bytes, String filenameHint);

    void deleteByPublicId(String publicId);
}
