package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordPaymentRequest(
    UUID invoiceId,
    @NotNull(message = "paymentMethod is required") PaymentMethod paymentMethod,
    @NotNull(message = "amount is required") @Positive(message = "amount must be positive") BigDecimal amount,
    String currency,
    String referenceNumber,
    String idempotencyKey,
    String notes
) {}

