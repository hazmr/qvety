package com.qvety.reference;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The colour is the day-view slot fill: six hex digits, or nothing and the day view picks one. */
@Schema(name = "AppointmentTypeRequest")
public record AppointmentTypeRequest(
    @NotBlank @Size(max = 100) String name,
    @Min(5) @Max(480) int durationMinutes,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{reference.color_invalid}") @Size(max = 7) String color
) {}
