package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.Refund;
import com.digitaloffice.finance.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
    UUID id,
    UUID paymentId,
    UUID organizationId,
    BigDecimal amount,
    String reason,
    RefundStatus status,
    UUID processedBy,
    Instant processedAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static RefundResponse from(Refund r) {
        return new RefundResponse(
            r.getId(),
            r.getPaymentId(),
            r.getOrganizationId(),
            r.getAmount(),
            r.getReason(),
            r.getStatus(),
            r.getProcessedBy(),
            r.getProcessedAt(),
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }
}

