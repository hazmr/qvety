package com.qvety.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** The only self-service profile field for now: the UI locale. */
@Schema(name = "UpdateMeRequest")
public record UpdateMeRequest(
    @NotBlank @Pattern(regexp = "ar-EG|en-EG", message = "{locale.unsupported}") String locale
) {}
