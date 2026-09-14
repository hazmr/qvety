package com.qvety.clients;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "Client")
public record ClientDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) String fullName,
    String preferredName,
    String phone,
    String phoneSecondary,
    String email,
    String address,
    String notes,
    String preferredLocale,
    OffsetDateTime archivedAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) long version,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime updatedAt
) {}
