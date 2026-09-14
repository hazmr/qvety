package com.qvety.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest")
public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
