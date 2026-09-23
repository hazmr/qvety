package com.qvety.platform;

import com.qvety.practice.PracticeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The reason is required: the audit row it writes is what answers "why were we suspended?". */
@Schema(name = "ChangeStatusRequest")
public record ChangeStatusRequest(
    @NotNull PracticeStatus status,
    @NotBlank @Size(max = 500) String reason
) {}
