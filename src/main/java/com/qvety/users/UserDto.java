package com.qvety.users;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;

@Schema(name = "User")
public record UserDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = RequiredMode.REQUIRED) String phone,
    String email,
    @Schema(requiredMode = RequiredMode.REQUIRED) String fullName,
    @Schema(requiredMode = RequiredMode.REQUIRED) UserRole role,
    boolean veterinarian,
    String licenseNumber,
    boolean active,
    boolean mustChangePassword,
    @Schema(requiredMode = RequiredMode.REQUIRED) String locale
) {}
