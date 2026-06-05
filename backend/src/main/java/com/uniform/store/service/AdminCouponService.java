package com.uniform.store.service;

import com.uniform.store.dto.request.CreateCouponRequest;
import com.uniform.store.dto.request.UpdateCouponRequest;
import com.uniform.store.dto.response.AdminCouponDto;
import com.uniform.store.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminCouponService {

    PageResponse<AdminCouponDto> list(String status, String search, Pageable pageable);

    AdminCouponDto get(Long id);

    AdminCouponDto create(CreateCouponRequest req);

    AdminCouponDto update(Long id, UpdateCouponRequest req);

    void delete(Long id);
}
