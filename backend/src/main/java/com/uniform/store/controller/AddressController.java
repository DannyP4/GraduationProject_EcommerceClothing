package com.uniform.store.controller;

import com.uniform.store.dto.request.CreateAddressRequest;
import com.uniform.store.dto.request.UpdateAddressRequest;
import com.uniform.store.dto.response.AddressDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "List current user's addresses (default first)")
    public ApiResponse<List<AddressDto>> list(Authentication authentication) {
        return ApiResponse.ok(addressService.listAddresses(authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create address (auto-default if user's first)")
    public ApiResponse<AddressDto> create(Authentication authentication,
                                          @Valid @RequestBody CreateAddressRequest req) {
        return ApiResponse.ok("Address created",
                addressService.createAddress(authentication.getName(), req));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update address (call /default to change default flag)")
    public ApiResponse<AddressDto> update(Authentication authentication,
                                          @PathVariable Long id,
                                          @Valid @RequestBody UpdateAddressRequest req) {
        return ApiResponse.ok(addressService.updateAddress(authentication.getName(), id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address (auto-promotes oldest remaining if this was default)")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        addressService.deleteAddress(authentication.getName(), id);
        return ApiResponse.ok("Address deleted", null);
    }

    @PostMapping("/{id}/default")
    @Operation(summary = "Promote this address to default (un-flags any previous default)")
    public ApiResponse<AddressDto> setDefault(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok("Default address updated",
                addressService.setDefault(authentication.getName(), id));
    }
}
