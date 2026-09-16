package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "WeightRequest")
public record WeightRequest(
    @NotNull @PastOrPresent OffsetDateTime measuredAt,
    @NotNull @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal weightKg
) {}
