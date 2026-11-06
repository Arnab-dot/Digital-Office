package com.digitaloffice.finance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID invoiceId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private int sortOrder;

    protected InvoiceLineItem() {}

    public InvoiceLineItem(UUID invoiceId, String description, int quantity, BigDecimal unitPrice, int sortOrder) {
        this.id = UUID.randomUUID();
        this.invoiceId = invoiceId;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public UUID getInvoiceId() { return invoiceId; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getAmount() { return amount; }
    public int getSortOrder() { return sortOrder; }
}
