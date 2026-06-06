package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.ShippingQuoteDto;
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
}
