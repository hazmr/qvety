package com.qvety.practice;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Read shape of a practice. Never the entity itself: the API stays stable when the table changes. */
@Schema(name = "Practice")   // generated Angular client adds the Dto suffix itself
public record PracticeDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) String name,
    @Schema(requiredMode = RequiredMode.REQUIRED) String country,
    @Schema(requiredMode = RequiredMode.REQUIRED) String currency,
    @Schema(requiredMode = RequiredMode.REQUIRED) String locale,
    @Schema(requiredMode = RequiredMode.REQUIRED) String timezone,
    @Schema(requiredMode = RequiredMode.REQUIRED) PracticeStatus status,
    String address,
    String phone,
    String vatNumber,
    @Schema(requiredMode = RequiredMode.REQUIRED) BigDecimal taxRatePercent,
    OffsetDateTime trialEndsAt
) {}
