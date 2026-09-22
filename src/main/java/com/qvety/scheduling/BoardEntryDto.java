package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.OffsetDateTime;
import java.util.UUID;

/** One card on the whiteboard. minutesWaiting counts from check-in; null before the patient arrives. */
@Schema(name = "BoardEntry")
public record BoardEntryDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) String patientName,
    @Schema(requiredMode = RequiredMode.REQUIRED) String clientName,
    @Schema(requiredMode = RequiredMode.REQUIRED) String veterinarianName,
    @Schema(requiredMode = RequiredMode.REQUIRED) String roomName,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime startsAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) AppointmentStatus status,
    @Schema(requiredMode = RequiredMode.REQUIRED) AppointmentOrigin origin,
    Integer minutesWaiting,
    String reason
) {}
