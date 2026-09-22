package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** The grid: one column per veterinarian, plus the opening hours that day so the view can shade them. */
@Schema(name = "Day")
public record DayDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) LocalDate date,
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "IANA zone of the practice; the client renders every instant in it") String timezone,
    LocalTime opens,
    LocalTime closes,
    @Schema(requiredMode = RequiredMode.REQUIRED) List<DayColumnDto> columns
) {}
