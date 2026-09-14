package com.qvety.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest")
/** identifier: phone in any Egyptian shape (01..., +20..., 0020...) or an email address. */
public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}
