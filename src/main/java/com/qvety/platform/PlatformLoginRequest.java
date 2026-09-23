package com.qvety.platform;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "PlatformLoginRequest")
public record PlatformLoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}
