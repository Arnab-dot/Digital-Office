package com.digitaloffice.finance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RefundRequest(
    @NotNull(message = "amount is required") @Positive(message = "amount must be positive") BigDecimal amount,
    String reason
) {}

