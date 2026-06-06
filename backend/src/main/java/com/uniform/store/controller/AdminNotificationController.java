package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.NotificationDto;
import com.uniform.store.service.AdminNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications")
@SecurityRequirement(name = "bearerAuth")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    @Operation(summary = "Derived admin notification feed: recent open orders, low-stock variants, recent reviews")
    public ApiResponse<List<NotificationDto>> feed() {
        return ApiResponse.ok(adminNotificationService.feed());
    }
}
