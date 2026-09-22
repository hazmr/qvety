package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "StatusChangeRequest")
public record StatusChangeRequest(@NotNull AppointmentStatus status) {}
