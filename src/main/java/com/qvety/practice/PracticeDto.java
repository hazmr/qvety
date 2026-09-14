package com.qvety.practice;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Read shape of a practice. Never the entity itself: the API stays stable when the table changes. */
@Schema(name = "Practice")   // generated Angular client adds the Dto suffix itself
public record PracticeDto(
    UUID id,
    String name,
    String country,
    String currency,
    String locale,
    String timezone,
    PracticeStatus status,
    String address,
    String phone,
    String vatNumber,
    BigDecimal taxRatePercent,
    OffsetDateTime trialEndsAt
) {}
