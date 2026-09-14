package com.qvety.tenant.audit;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(name = "AuditEntry")
public record AuditEntryDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    UUID userId,
    @Schema(requiredMode = RequiredMode.REQUIRED) String tableName,
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID rowId,
    @Schema(requiredMode = RequiredMode.REQUIRED) AuditAction action,
    Map<String, Object> before,
    Map<String, Object> after,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime at
) {}
