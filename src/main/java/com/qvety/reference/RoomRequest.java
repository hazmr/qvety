package com.qvety.reference;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create and update share one shape. active is not here: deactivate and activate are their own endpoints. */
@Schema(name = "RoomRequest")
public record RoomRequest(
    @NotBlank @Size(max = 100) String name
) {}
