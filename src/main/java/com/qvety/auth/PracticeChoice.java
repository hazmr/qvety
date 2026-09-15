package com.qvety.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;

/** One practice the caller may log in to, shown when the same phone or email matched at several practices. */
@Schema(name = "PracticeChoice")
public record PracticeChoice(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) String name
) {}
