package com.qvety.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin creates a user with a temporary password; the user must change it at first login. */
@Schema(name = "UserCreateRequest")
public record UserCreateRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 10, max = 200) String temporaryPassword,
    @NotBlank String fullName,
    @NotNull UserRole role,
    boolean veterinarian,
    String licenseNumber,
    String phone
) {}
