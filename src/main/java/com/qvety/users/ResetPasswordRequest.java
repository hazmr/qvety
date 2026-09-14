package com.qvety.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ResetPasswordRequest")
public record ResetPasswordRequest(@NotBlank @Size(min = 10, max = 200) String temporaryPassword) {}
