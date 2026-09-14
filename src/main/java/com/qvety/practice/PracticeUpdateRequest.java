package com.qvety.practice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Identity columns only. Country, currency, and status are not here: the super admin owns them. */
@Schema(name = "PracticeUpdateRequest")
public record PracticeUpdateRequest(
    @NotBlank String name,
    String address,
    String phone,
    String vatNumber,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal taxRatePercent
) {}
