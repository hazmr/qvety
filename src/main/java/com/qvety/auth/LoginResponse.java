package com.qvety.auth;

import com.qvety.users.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "LoginResponse")
public record LoginResponse(
    @Schema(requiredMode = RequiredMode.REQUIRED) String token,
    @Schema(requiredMode = RequiredMode.REQUIRED) UserDto user
) {}
