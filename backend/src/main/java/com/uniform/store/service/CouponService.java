package com.uniform.store.service;

import com.uniform.store.dto.response.CouponValidationResponse;
import com.uniform.store.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    CouponValidationResponse validate(String email, String code);

    CouponApplication applyToOrder(String code, Long userId, List<CartLine> lines, BigDecimal subtotal);

    void releaseForOrder(Long orderId);

    record CartLine(Long productId, Long categoryId, BigDecimal lineTotal) {}

    record CouponApplication(Coupon coupon, BigDecimal discountAmount) {}
}
