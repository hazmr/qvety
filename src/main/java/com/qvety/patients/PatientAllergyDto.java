package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "PatientAllergy")
public record PatientAllergyDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID patientId,
    @Schema(requiredMode = RequiredMode.REQUIRED) String substance,
    String reaction,
    @Schema(requiredMode = RequiredMode.REQUIRED) AllergySeverity severity,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID notedBy,
    OffsetDateTime retractedAt,
    String retractedReason,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt
) {}
