package com.qvety.reference;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** No currency: it is the practice currency, and the server sets it. Zero is allowed (a free re-check). */
@Schema(name = "ServiceRequest")
public record ServiceRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal price
) {}
