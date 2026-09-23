package com.qvety.platform;

import com.qvety.practice.Practice;
import com.qvety.practice.PracticeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A practice as Qvety sees it: identity, where it is in its lifecycle, and nothing clinical. */
@Schema(name = "PracticeSummary")
public record PracticeSummaryDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) String name,
    @Schema(requiredMode = RequiredMode.REQUIRED) String country,
    @Schema(requiredMode = RequiredMode.REQUIRED) String currency,
    @Schema(requiredMode = RequiredMode.REQUIRED) PracticeStatus status,
    OffsetDateTime trialEndsAt,
    OffsetDateTime closedAt,
    @Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt
) {

    static PracticeSummaryDto of(Practice p) {
        return new PracticeSummaryDto(p.getId(), p.getName(), p.getCountry(), p.getCurrency(),
            p.getStatus(), p.getTrialEndsAt(), p.getClosedAt(), p.getCreatedAt());
    }
}
