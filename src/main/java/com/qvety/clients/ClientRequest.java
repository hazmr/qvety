package com.qvety.clients;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create and update share one shape. The required phone is a service rule (client.phone_required), not a field rule. */
@Schema(name = "ClientRequest")
public record ClientRequest(
    @NotBlank @Size(max = 200) String fullName,
    @Size(max = 100) String preferredName,
    @Size(max = 30) String phone,
    @Size(max = 30) String phoneSecondary,
    @Email @Size(max = 200) String email,
    @Size(max = 500) String address,
    @Size(max = 2000) String notes,
    @Pattern(regexp = "ar-EG|en-EG", message = "{locale.unsupported}") String preferredLocale
) {}
