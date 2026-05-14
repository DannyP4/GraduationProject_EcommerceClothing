package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/vnpay/verify")
    @Operation(summary = "Verify VNPAY return-url signature and transition order (idempotent)")
    public ApiResponse<PaymentService.VnpayReturnResult> verifyVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (v != null && v.length > 0) params.put(k, v[0]);
        });
        PaymentService.VnpayReturnResult result = paymentService.handleVnpayReturn(params);
        return result.success()
                ? ApiResponse.ok(result.message(), result)
                : ApiResponse.ok(result.message(), result);
    }

    @PostMapping("/stripe/webhook")
    @Operation(summary = "Stripe checkout.session.completed webhook receiver")
    public ResponseEntity<String> stripeWebhook(@RequestBody String payload,
                                                 @RequestHeader("Stripe-Signature") String signature) {
        paymentService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/{orderNumber}/retry")
    @Operation(summary = "Re-initiate payment for a PENDING VNPAY/Stripe order")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PaymentService.RetryResult> retry(Authentication authentication,
                                                          @PathVariable String orderNumber,
                                                          HttpServletRequest http) {
        return ApiResponse.ok("Payment retry initiated",
                paymentService.retryPayment(authentication.getName(), orderNumber, resolveClientIp(http)));
    }

    private static String resolveClientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
