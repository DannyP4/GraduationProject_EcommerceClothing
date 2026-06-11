package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.NotificationResponse;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List the current user's notifications (newest first)")
    public ApiResponse<PageResponse<NotificationResponse>> list(Authentication authentication,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return ApiResponse.ok(notificationService.listForUser(authentication.getName(), pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count the current user's unread notifications")
    public ApiResponse<Long> unreadCount(Authentication authentication) {
        return ApiResponse.ok(notificationService.unreadCount(authentication.getName()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ApiResponse<Void> markRead(Authentication authentication, @PathVariable Long id) {
        notificationService.markRead(authentication.getName(), id);
        return ApiResponse.ok("Marked read", null);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all of the current user's notifications as read")
    public ApiResponse<Integer> markAllRead(Authentication authentication) {
        return ApiResponse.ok("Marked all read", notificationService.markAllRead(authentication.getName()));
    }
}
