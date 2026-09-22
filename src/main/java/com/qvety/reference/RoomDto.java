package com.qvety.reference;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "Room")
public record RoomDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) String name,
    @Schema(requiredMode = RequiredMode.REQUIRED) boolean active,
    @Schema(requiredMode = RequiredMode.REQUIRED) long version,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime updatedAt
) {}
