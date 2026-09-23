package com.qvety.platform;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The first admin is named by phone; email is optional, because a small Egyptian clinic often has none.
 * There is no password field: the server generates a temporary one and returns it once.
 */
@Schema(name = "CreatePracticeRequest")
public record CreatePracticeRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "{platform.country_invalid}") String country,
    @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "{platform.currency_invalid}") String currency,
    @NotBlank @Pattern(regexp = "ar-EG|en-EG", message = "{locale.unsupported}") String locale,
    @NotBlank @Size(max = 64) String timezone,
    @NotBlank @Size(max = 200) String adminName,
    @NotBlank @Size(max = 30) String adminPhone,
    @Email @Size(max = 200) String adminEmail,
    @Schema(description = "Whether the first admin is also a veterinarian; most clinic owners are.") Boolean adminVeterinarian
) {

    /**
     * Boxed on purpose: Jackson refuses a whole body that leaves a primitive record component out, so an
     * absent flag would answer malformed_request instead of meaning "no". Same trap as part 10's walkIn.
     */
    public boolean isAdminVeterinarian() {
        return Boolean.TRUE.equals(adminVeterinarian);
    }
}
