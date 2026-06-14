package com.uniform.store.service;

import com.uniform.store.enums.TryOnStatus;

public interface VirtualTryOnProvider {

    boolean isEnabled();

    String name();

    Submission submit(String modelImageUrl, String garmentImageUrl, String garmentPhotoType, String category);

    PollResult poll(String responseUrl);

    record Submission(String requestId, String responseUrl) {}

    record PollResult(TryOnStatus status, String resultImageUrl, String error) {}
}
