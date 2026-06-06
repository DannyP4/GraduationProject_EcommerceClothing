package com.uniform.store.controller;

import com.uniform.store.dto.request.CouponValidateRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.CouponValidationResponse;
import com.uniform.store.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons")
@SecurityRequirement(name = "bearerAuth")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    @Operation(summary = "Validate a coupon and preview the discount against the cart (or a Buy Now line)")
    public ApiResponse<CouponValidationResponse> validate(Authentication authentication,
                                                          @Valid @RequestBody CouponValidateRequest req) {
        CouponValidationResponse resp = req.getVariantId() != null
                ? couponService.validateDirect(authentication.getName(), req.getCode(),
                        req.getVariantId(), req.getQuantity() == null ? 1 : req.getQuantity())
                : couponService.validate(authentication.getName(), req.getCode());
        return ApiResponse.ok(resp);
    }
}
