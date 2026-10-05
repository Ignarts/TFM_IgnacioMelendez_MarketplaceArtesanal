package com.marketplace.report.dto;

import com.marketplace.report.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull ReportTargetType type,
        @NotNull Long targetId,
        @NotBlank @Size(max = 500) String reason
) {}
