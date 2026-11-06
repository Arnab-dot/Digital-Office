package com.digitaloffice.finance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String referenceNumber;
    private String idempotencyKey;
    private UUID confirmedBy;
    private Instant confirmedAt;
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Payment() {}

    public Payment(UUID organizationId, UUID invoiceId, PaymentMethod paymentMethod,
                   BigDecimal amount, String currency, String referenceNumber,
                   String idempotencyKey, String notes) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.invoiceId = invoiceId;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PENDING;
        this.amount = amount;
        this.currency = currency;
        this.referenceNumber = referenceNumber;
        this.idempotencyKey = idempotencyKey;
        this.notes = notes;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getInvoiceId() { return invoiceId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getConfirmedBy() { return confirmedBy; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void confirm(UUID confirmedBy) {
        this.status = PaymentStatus.CONFIRMED;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }
}
