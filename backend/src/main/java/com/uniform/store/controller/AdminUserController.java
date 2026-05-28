package com.uniform.store.controller;

import com.uniform.store.dto.request.AdminUserFilterRequest;
import com.uniform.store.dto.response.AdminUserDetailDto;
import com.uniform.store.dto.response.AdminUserSummaryDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List users with optional status / search filter")
    public ApiResponse<PageResponse<AdminUserSummaryDto>> list(
            @ModelAttribute AdminUserFilterRequest filter,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(adminUserService.list(filter, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user detail with addresses and recent orders")
    public ApiResponse<AdminUserDetailDto> get(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.get(id));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend a user (blocks login until activated)")
    public ApiResponse<AdminUserDetailDto> suspend(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok("User suspended", adminUserService.suspend(id, auth.getName()));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a suspended or deleted user")
    public ApiResponse<AdminUserDetailDto> activate(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok("User activated", adminUserService.activate(id, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a user (sets status=DELETED, preserves row for order history + PII)")
    public ApiResponse<AdminUserDetailDto> softDelete(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok("User soft-deleted", adminUserService.softDelete(id, auth.getName()));
    }
}
