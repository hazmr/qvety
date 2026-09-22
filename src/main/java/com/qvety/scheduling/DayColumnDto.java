package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;
import java.util.UUID;

/** One veterinarian's column in the day grid. */
@Schema(name = "DayColumn")
public record DayColumnDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID veterinarianId,
    @Schema(requiredMode = RequiredMode.REQUIRED) String veterinarianName,
    @Schema(requiredMode = RequiredMode.REQUIRED) List<AppointmentDto> appointments
) {}
