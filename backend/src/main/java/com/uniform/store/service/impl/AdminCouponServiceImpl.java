package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateCouponRequest;
import com.uniform.store.dto.request.UpdateCouponRequest;
import com.uniform.store.dto.response.AdminCouponDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.entity.Coupon;
import com.uniform.store.enums.CouponScope;
import com.uniform.store.enums.CouponStatus;
import com.uniform.store.enums.CouponType;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.CouponRepository;
import com.uniform.store.repository.OrderCouponRepository;
import com.uniform.store.service.AdminCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCouponServiceImpl implements AdminCouponService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CouponRepository couponRepository;
    private final OrderCouponRepository orderCouponRepository;

    @Override
    public PageResponse<AdminCouponDto> list(String status, String search, Pageable pageable) {
        CouponStatus statusFilter = parseStatus(status);
        String q = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Coupon> page = couponRepository.searchAdmin(statusFilter, q, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toSummaryDto).toList());
    }

    @Override
    public AdminCouponDto get(Long id) {
        return toDetailDto(load(id));
    }

    @Override
    @Transactional
    public AdminCouponDto create(CreateCouponRequest req) {
        String code = normalize(req.getCode());
        if (couponRepository.existsByCode(code)) {
            throw new BadRequestException("Coupon code already exists: " + code);
        }
        CouponScope scope = req.getScope() == null ? CouponScope.ALL : req.getScope();
        validate(req.getType(), req.getValue(), scope, req.getStartsAt(), req.getEndsAt(),
                req.getCategoryIds(), req.getProductIds());

        Coupon coupon = Coupon.builder()
                .code(code)
                .type(req.getType())
                .value(req.getValue())
                .scope(scope)
                .minOrderAmount(req.getMinOrderAmount())
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .maxUses(req.getMaxUses())
                .maxUsesPerUser(req.getMaxUsesPerUser())
                .status(req.getStatus() == null ? CouponStatus.ACTIVE : req.getStatus())
                .categoryIds(scope == CouponScope.CATEGORY ? sanitize(req.getCategoryIds()) : new LinkedHashSet<>())
                .productIds(scope == CouponScope.PRODUCT ? sanitize(req.getProductIds()) : new LinkedHashSet<>())
                .build();
        return toDetailDto(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public AdminCouponDto update(Long id, UpdateCouponRequest req) {
        Coupon coupon = load(id);
        CouponScope scope = req.getScope() == null ? CouponScope.ALL : req.getScope();
        validate(req.getType(), req.getValue(), scope, req.getStartsAt(), req.getEndsAt(),
                req.getCategoryIds(), req.getProductIds());

        coupon.setType(req.getType());
        coupon.setValue(req.getValue());
        coupon.setScope(scope);
        coupon.setMinOrderAmount(req.getMinOrderAmount());
        coupon.setMaxDiscountAmount(req.getMaxDiscountAmount());
        coupon.setStartsAt(req.getStartsAt());
        coupon.setEndsAt(req.getEndsAt());
        coupon.setMaxUses(req.getMaxUses());
        coupon.setMaxUsesPerUser(req.getMaxUsesPerUser());
        coupon.setStatus(req.getStatus());
        coupon.setCategoryIds(scope == CouponScope.CATEGORY ? sanitize(req.getCategoryIds()) : new LinkedHashSet<>());
        coupon.setProductIds(scope == CouponScope.PRODUCT ? sanitize(req.getProductIds()) : new LinkedHashSet<>());
        return toDetailDto(coupon);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Coupon coupon = load(id);
        if (orderCouponRepository.existsByCouponId(id)) {
            throw new BadRequestException(
                    "Cannot delete a coupon that has been used on orders. Disable it instead.");
        }
        couponRepository.delete(coupon);
    }

    private Coupon load(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", id));
    }

    private void validate(CouponType type, BigDecimal value, CouponScope scope,
                          Instant startsAt, Instant endsAt, Set<Long> categoryIds, Set<Long> productIds) {
        if (value == null || value.signum() <= 0) {
            throw new BadRequestException("Coupon value must be greater than 0.");
        }
        if (type == CouponType.PERCENT && value.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("PERCENT coupon value cannot exceed 100.");
        }
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BadRequestException("endsAt must be after startsAt.");
        }
        if (scope == CouponScope.CATEGORY && sanitize(categoryIds).isEmpty()) {
            throw new BadRequestException("CATEGORY scope requires at least one categoryId.");
        }
        if (scope == CouponScope.PRODUCT && sanitize(productIds).isEmpty()) {
            throw new BadRequestException("PRODUCT scope requires at least one productId.");
        }
    }

    private static Set<Long> sanitize(Set<Long> ids) {
        Set<Long> out = new LinkedHashSet<>();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null) out.add(id);
            }
        }
        return out;
    }

    private static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private CouponStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return CouponStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status filter: expected ACTIVE or DISABLED");
        }
    }

    private AdminCouponDto toSummaryDto(Coupon c) {
        return baseDto(c).build();
    }

    private AdminCouponDto toDetailDto(Coupon c) {
        return baseDto(c)
                .categoryIds(new LinkedHashSet<>(c.getCategoryIds()))
                .productIds(new LinkedHashSet<>(c.getProductIds()))
                .build();
    }

    private AdminCouponDto.AdminCouponDtoBuilder baseDto(Coupon c) {
        return AdminCouponDto.builder()
                .id(c.getId())
                .code(c.getCode())
                .type(c.getType())
                .value(c.getValue())
                .scope(c.getScope())
                .minOrderAmount(c.getMinOrderAmount())
                .maxDiscountAmount(c.getMaxDiscountAmount())
                .startsAt(c.getStartsAt())
                .endsAt(c.getEndsAt())
                .maxUses(c.getMaxUses())
                .maxUsesPerUser(c.getMaxUsesPerUser())
                .usedCount(c.getUsedCount())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt());
    }
}
