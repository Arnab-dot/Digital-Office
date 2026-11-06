package com.digitaloffice.finance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private UUID organizationId;

    private UUID subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amountPaid;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amountDue;

    private Instant billingPeriodStart;
    private Instant billingPeriodEnd;
    private Instant issuedAt;
    private Instant dueDate;
    private Instant paidAt;
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private int version;

    protected Invoice() {}

    public Invoice(String invoiceNumber, UUID organizationId, UUID subscriptionId, String currency,
                   BigDecimal taxRate, Instant billingPeriodStart, Instant billingPeriodEnd, Instant dueDate) {
        this.id = UUID.randomUUID();
        this.invoiceNumber = invoiceNumber;
        this.organizationId = organizationId;
        this.subscriptionId = subscriptionId;
        this.status = InvoiceStatus.DRAFT;
        this.currency = currency;
        this.taxRate = taxRate;
        this.subtotal = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.amountPaid = BigDecimal.ZERO;
        this.amountDue = BigDecimal.ZERO;
        this.billingPeriodStart = billingPeriodStart;
        this.billingPeriodEnd = billingPeriodEnd;
        this.dueDate = dueDate;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Getters
    public UUID getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public InvoiceStatus getStatus() { return status; }
    public String getCurrency() { return currency; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getAmountDue() { return amountDue; }
    public Instant getBillingPeriodStart() { return billingPeriodStart; }
    public Instant getBillingPeriodEnd() { return billingPeriodEnd; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getDueDate() { return dueDate; }
    public Instant getPaidAt() { return paidAt; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    // Financial calculations
    public void recalculate(BigDecimal newSubtotal, BigDecimal discount) {
        this.subtotal = newSubtotal;
        this.discountAmount = discount;
        BigDecimal taxable = newSubtotal.subtract(discount);
        this.taxAmount = taxable.multiply(this.taxRate);
        this.total = taxable.add(this.taxAmount);
        this.amountDue = this.total.subtract(this.amountPaid);
        this.updatedAt = Instant.now();
    }

    public void issue() {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be issued");
        }
        this.status = InvoiceStatus.ISSUED;
        this.issuedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void recordPayment(BigDecimal amount) {
        this.amountPaid = this.amountPaid.add(amount);
        this.amountDue = this.total.subtract(this.amountPaid);
        if (this.amountDue.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = InvoiceStatus.PAID;
            this.paidAt = Instant.now();
            this.amountDue = BigDecimal.ZERO;
        } else {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
        this.updatedAt = Instant.now();
    }

    public void markOverdue() {
        if (this.status == InvoiceStatus.ISSUED || this.status == InvoiceStatus.PARTIALLY_PAID) {
            this.status = InvoiceStatus.OVERDUE;
            this.updatedAt = Instant.now();
        }
    }

    public void voidInvoice() {
        this.status = InvoiceStatus.VOID;
        this.updatedAt = Instant.now();
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = Instant.now();
    }
}
