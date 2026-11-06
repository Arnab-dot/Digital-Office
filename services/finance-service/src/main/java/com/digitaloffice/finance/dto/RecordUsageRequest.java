package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.UsageMetric;
import jakarta.validation.constraints.NotNull;

public record RecordUsageRequest(
    @NotNull(message = "metric is required") UsageMetric metric,
    long value,
    boolean setAbsolute
) {}

