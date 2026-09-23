package com.qvety.platform;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "PlatformLoginResponse")
public record PlatformLoginResponse(
    @Schema(requiredMode = RequiredMode.REQUIRED) String token,
    @Schema(requiredMode = RequiredMode.REQUIRED) String fullName,
    @Schema(requiredMode = RequiredMode.REQUIRED) String email
) {}
