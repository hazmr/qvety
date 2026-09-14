package com.qvety.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UserUpdateRequest")
public record UserUpdateRequest(
    @NotBlank String fullName,
    @NotNull UserRole role,
    boolean veterinarian,
    String licenseNumber,
    String phone
) {}
