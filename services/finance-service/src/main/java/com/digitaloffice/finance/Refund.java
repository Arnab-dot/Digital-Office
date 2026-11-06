package com.digitaloffice.finance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    private UUID processedBy;
    private Instant processedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Refund() {}

    public Refund(UUID paymentId, UUID organizationId, BigDecimal amount, String reason) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.organizationId = organizationId;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getOrganizationId() { return organizationId; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public RefundStatus getStatus() { return status; }
    public UUID getProcessedBy() { return processedBy; }
    public Instant getProcessedAt() { return processedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void process(UUID processedBy) {
        this.status = RefundStatus.PROCESSED;
        this.processedBy = processedBy;
        this.processedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void reject(UUID processedBy) {
        this.status = RefundStatus.REJECTED;
        this.processedBy = processedBy;
        this.processedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
