package com.qvety.common;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Map;

/** The one error shape. code is stable and machine-readable; message is localized per Accept-Language. */
@Schema(name = "ApiError")
public record ApiError(
    @Schema(requiredMode = RequiredMode.REQUIRED) String code,
    @Schema(requiredMode = RequiredMode.REQUIRED) String message,
    Map<String, String> fields
) {}
