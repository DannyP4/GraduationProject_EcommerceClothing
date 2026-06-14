package com.uniform.store.service;

import com.uniform.store.dto.request.TryOnCreateRequest;
import com.uniform.store.dto.response.TryOnJobDto;

public interface VirtualTryOnService {

    TryOnJobDto createJob(String userEmail, TryOnCreateRequest req);

    TryOnJobDto getJob(String userEmail, Long jobId);
}
