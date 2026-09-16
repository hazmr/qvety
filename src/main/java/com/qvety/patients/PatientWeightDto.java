package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "PatientWeight")
public record PatientWeightDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID patientId,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime measuredAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) BigDecimal weightKg,
    OffsetDateTime voidedAt,
    String voidReason,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt
) {}
