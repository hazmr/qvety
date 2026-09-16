package com.qvety.patients;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "TransferRequest")
public record TransferRequest(@NotNull UUID clientId) {}
