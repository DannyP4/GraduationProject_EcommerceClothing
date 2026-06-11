package com.uniform.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminInviteRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 150)
    private String fullName;
}
