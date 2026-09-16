package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RetractRequest")
public record RetractRequest(@NotBlank @Size(max = 500) String reason) {}
