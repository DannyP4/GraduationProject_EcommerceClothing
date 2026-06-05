package com.uniform.store.controller;

import com.uniform.store.dto.request.CreateCouponRequest;
import com.uniform.store.dto.request.UpdateCouponRequest;
import com.uniform.store.dto.response.AdminCouponDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.service.AdminCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Admin Coupons")
@SecurityRequirement(name = "bearerAuth")
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @GetMapping
    @Operation(summary = "List coupons (filter by status + code search)")
    public ApiResponse<PageResponse<AdminCouponDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(adminCouponService.list(status, search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon detail including scope targets")
    public ApiResponse<AdminCouponDto> get(@PathVariable Long id) {
        return ApiResponse.ok(adminCouponService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coupon")
    public ApiResponse<AdminCouponDto> create(@Valid @RequestBody CreateCouponRequest req) {
        return ApiResponse.ok("Coupon created", adminCouponService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a coupon (code is immutable)")
    public ApiResponse<AdminCouponDto> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateCouponRequest req) {
        return ApiResponse.ok("Coupon updated", adminCouponService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a coupon (blocked if used on orders — disable instead)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminCouponService.delete(id);
        return ApiResponse.ok("Coupon deleted", null);
    }
}
