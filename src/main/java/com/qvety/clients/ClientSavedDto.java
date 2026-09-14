package com.qvety.clients;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

/** Create/update response: the saved client plus possible duplicates. Warnings never block. */
@Schema(name = "ClientSaved")
public record ClientSavedDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) ClientDto client,
    @Schema(requiredMode = RequiredMode.REQUIRED) List<ClientDto> warnings
) {}
