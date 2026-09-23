package com.qvety.users;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;

/**
 * Just enough of a veterinarian to pick one. The full user record is admin-only; booking a visit is
 * front-desk work, so the booking form reads this instead.
 */
@Schema(name = "Veterinarian")
public record VeterinarianDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) String fullName
) {}
