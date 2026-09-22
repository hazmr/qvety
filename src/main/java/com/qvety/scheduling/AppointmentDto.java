package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Times go out as instants; the browser renders them in the practice timezone. */
@Schema(name = "Appointment")
public record AppointmentDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID patientId,
    @Schema(requiredMode = RequiredMode.REQUIRED) String patientName,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID clientId,
    @Schema(requiredMode = RequiredMode.REQUIRED) String clientName,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID veterinarianId,
    @Schema(requiredMode = RequiredMode.REQUIRED) String veterinarianName,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID roomId,
    @Schema(requiredMode = RequiredMode.REQUIRED) String roomName,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID appointmentTypeId,
    @Schema(requiredMode = RequiredMode.REQUIRED) String appointmentTypeName,
    String color,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime startsAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime endsAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) AppointmentStatus status,
    @Schema(requiredMode = RequiredMode.REQUIRED) AppointmentOrigin origin,
    OffsetDateTime checkedInAt,
    String reason,
    String notes,
    @Schema(requiredMode = RequiredMode.REQUIRED) long version
) {}
