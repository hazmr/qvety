package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Create and update share one shape. The desk picks a start and a length, not an end: the appointment
 * type supplies the default length and the dialog lets it be changed. walkIn only applies on create.
 *
 * Both optional-looking numbers are boxed on purpose: Jackson refuses a whole body that leaves a
 * primitive record component out, so a missing walkIn would answer malformed_request instead of
 * defaulting to false, and a missing durationMinutes would hide a plain validation error.
 */
@Schema(name = "AppointmentRequest")
public record AppointmentRequest(
    @NotNull UUID patientId,
    @NotNull UUID veterinarianId,
    @NotNull UUID roomId,
    @NotNull UUID appointmentTypeId,
    @NotNull OffsetDateTime start,
    @NotNull @Min(5) @Max(480) Integer durationMinutes,
    @Size(max = 500) String reason,
    @Size(max = 2000) String notes,
    @Schema(description = "Create the row already checked in, with origin walk_in. Ignored on update.") Boolean walkIn
) {

    /** Absent means a normal booking. */
    public boolean isWalkIn() {
        return Boolean.TRUE.equals(walkIn);
    }
}
