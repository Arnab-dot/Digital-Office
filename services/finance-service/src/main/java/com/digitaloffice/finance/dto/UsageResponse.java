package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.UsageMetric;

import java.time.Instant;

public record UsageResponse(
    UsageMetric metric,
    long currentValue,
    long limit,
    long remainingQuota,
    Instant periodStart,
    Instant periodEnd
) {}

