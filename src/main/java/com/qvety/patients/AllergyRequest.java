package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "AllergyRequest")
public record AllergyRequest(
    @NotBlank @Size(max = 200) String substance,
    @Size(max = 1000) String reaction,
    @NotNull AllergySeverity severity
) {}
