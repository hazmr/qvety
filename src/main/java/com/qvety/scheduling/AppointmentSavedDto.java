package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

/**
 * The saved appointment plus anything the desk should know but that must not block the booking.
 * Warnings are message codes the client localizes (currently only outside opening hours).
 */
@Schema(name = "AppointmentSaved")
public record AppointmentSavedDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) AppointmentDto appointment,
    @Schema(requiredMode = RequiredMode.REQUIRED) List<String> warnings
) {}
