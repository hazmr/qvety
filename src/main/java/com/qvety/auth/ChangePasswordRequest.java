package com.qvety.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ChangePasswordRequest")
public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 10, max = 200) String newPassword
) {}
