package com.qvety.platform;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Returned once, at creation. The temporary password is never readable again: the super admin hands it
 * over in person or by WhatsApp, and the owner must change it at first login.
 */
@Schema(name = "PracticeCreated")
public record PracticeCreatedDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) PracticeSummaryDto practice,
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "The admin's login identifier, normalized to E.164") String adminPhone,
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "Shown once; not stored in readable form") String temporaryPassword
) {}
