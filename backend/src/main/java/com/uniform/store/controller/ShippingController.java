package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.DistrictDto;
import com.uniform.store.dto.response.ProvinceDto;
import com.uniform.store.dto.response.ShippingQuoteDto;
import com.uniform.store.dto.response.WardDto;
import com.uniform.store.enums.ShippingRegion;
import com.uniform.store.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping")
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping("/quote")
    @Operation(summary = "Shipping fee for a region and order subtotal (free at/above the threshold)")
    public ApiResponse<ShippingQuoteDto> quote(
            @RequestParam(required = false) ShippingRegion region,
            @RequestParam(required = false) BigDecimal subtotal) {
        return ApiResponse.ok(ShippingQuoteDto.builder()
                .fee(shippingService.fee(region, subtotal))
                .freeThreshold(shippingService.freeThreshold())
                .build());
    }

    @GetMapping("/provinces")
    @Operation(summary = "GHN provinces")
    public ApiResponse<List<ProvinceDto>> provinces() {
        return ApiResponse.ok(shippingService.provinces());
    }

    @GetMapping("/districts")
    @Operation(summary = "GHN districts for a province")
    public ApiResponse<List<DistrictDto>> districts(@RequestParam int provinceId) {
        return ApiResponse.ok(shippingService.districts(provinceId));
    }

    @GetMapping("/wards")
    @Operation(summary = "GHN wards for a district")
    public ApiResponse<List<WardDto>> wards(@RequestParam int districtId) {
        return ApiResponse.ok(shippingService.wards(districtId));
    }

    @GetMapping("/ghn-quote")
    @Operation(summary = "Shipping fee from GHN for a destination district/ward (free at/above the threshold)")
    public ApiResponse<ShippingQuoteDto> ghnQuote(
            @RequestParam Integer toDistrictId,
            @RequestParam String toWardCode,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(required = false) BigDecimal subtotal) {
        return ApiResponse.ok(ShippingQuoteDto.builder()
                .fee(shippingService.ghnFee(toDistrictId, toWardCode, quantity, subtotal))
                .freeThreshold(shippingService.freeThreshold())
                .build());
    }
}
