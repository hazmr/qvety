package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "Patient")
public record PatientDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID clientId,
    UUID previousClientId,
    @Schema(requiredMode = RequiredMode.REQUIRED) String name,
    @Schema(requiredMode = RequiredMode.REQUIRED) Species species,
    String breed,
    @Schema(requiredMode = RequiredMode.REQUIRED) PatientSex sex,
    LocalDate dateOfBirth,
    String ageApproximate,
    String color,
    String microchip,
    String photoObjectKey,
    LocalDate deceasedAt,
    String notes,
    @Schema(requiredMode = RequiredMode.REQUIRED) long version,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime updatedAt
) {}
