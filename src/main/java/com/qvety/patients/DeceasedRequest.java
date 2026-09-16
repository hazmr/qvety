package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

@Schema(name = "DeceasedRequest")
public record DeceasedRequest(@NotNull @PastOrPresent LocalDate date) {}
