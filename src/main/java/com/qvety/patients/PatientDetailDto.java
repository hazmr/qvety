package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

/** One read for the patient header: identity, latest weight, and the active allergies for the banner. */
@Schema(name = "PatientDetail")
public record PatientDetailDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) PatientDto patient,
    PatientWeightDto latestWeight,
    @Schema(requiredMode = RequiredMode.REQUIRED) List<PatientAllergyDto> activeAllergies
) {}
