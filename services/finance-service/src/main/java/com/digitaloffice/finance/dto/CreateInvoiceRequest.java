package com.digitaloffice.finance.dto;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
    UUID subscriptionId,
    String currency,
    BigDecimal taxRate,
    Instant billingPeriodStart,
    Instant billingPeriodEnd,
    Instant dueDate,
    @Valid List<CreateLineItemRequest> items
) {}

