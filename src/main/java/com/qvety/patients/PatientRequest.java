package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Create and update share one shape. clientId is set on create; on update it must equal the current owner,
 * because a change of owner is a transfer (its own endpoint, keeps previous_client_id).
 */
@Schema(name = "PatientRequest")
public record PatientRequest(
    @NotNull UUID clientId,
    @NotBlank @Size(max = 100) String name,
    @NotNull Species species,
    @Size(max = 100) String breed,
    PatientSex sex,
    @PastOrPresent LocalDate dateOfBirth,
    @Size(max = 50) String ageApproximate,
    @Size(max = 50) String color,
    @Size(max = 30) String microchip,
    @Size(max = 2000) String notes
) {}
