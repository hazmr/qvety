package com.qvety.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * identifier: phone in any Egyptian shape (01..., +20..., 0020...) or an email address.
 * practiceId: only on the second call, after a response carried practices; it narrows an already
 * verified match to one practice and never widens access.
 */
@Schema(name = "LoginRequest")
public record LoginRequest(@NotBlank String identifier, @NotBlank String password, UUID practiceId) {}
