package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.Payment;
import com.digitaloffice.finance.PaymentMethod;
import com.digitaloffice.finance.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID organizationId,
    UUID invoiceId,
    PaymentMethod paymentMethod,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String referenceNumber,
    String idempotencyKey,
    UUID confirmedBy,
    Instant confirmedAt,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
            p.getId(),
            p.getOrganizationId(),
            p.getInvoiceId(),
            p.getPaymentMethod(),
            p.getStatus(),
            p.getAmount(),
            p.getCurrency(),
            p.getReferenceNumber(),
            p.getIdempotencyKey(),
            p.getConfirmedBy(),
            p.getConfirmedAt(),
            p.getNotes(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}

