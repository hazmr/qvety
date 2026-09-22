package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

/** The whiteboard: today's non-cancelled appointments in the three columns the clinic reads. */
@Schema(name = "Board")
public record BoardDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) List<BoardEntryDto> waiting,
    @Schema(requiredMode = RequiredMode.REQUIRED) List<BoardEntryDto> inExam,
    @Schema(requiredMode = RequiredMode.REQUIRED) List<BoardEntryDto> done
) {}
