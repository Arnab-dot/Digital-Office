package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.Invoice;
import com.digitaloffice.finance.InvoiceLineItem;
import com.digitaloffice.finance.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    String invoiceNumber,
    UUID organizationId,
    UUID subscriptionId,
    InvoiceStatus status,
    String currency,
    BigDecimal subtotal,
    BigDecimal taxRate,
    BigDecimal taxAmount,
    BigDecimal discountAmount,
    BigDecimal total,
    BigDecimal amountPaid,
    BigDecimal amountDue,
    Instant billingPeriodStart,
    Instant billingPeriodEnd,
    Instant issuedAt,
    Instant dueDate,
    Instant paidAt,
    String notes,
    List<InvoiceLineItemResponse> lineItems,
    Instant createdAt,
    Instant updatedAt
) {
    public static InvoiceResponse from(Invoice inv, List<InvoiceLineItem> items) {
        List<InvoiceLineItemResponse> itemResponses = items != null
            ? items.stream().map(InvoiceLineItemResponse::from).toList()
            : List.of();
        return new InvoiceResponse(
            inv.getId(),
            inv.getInvoiceNumber(),
            inv.getOrganizationId(),
            inv.getSubscriptionId(),
            inv.getStatus(),
            inv.getCurrency(),
            inv.getSubtotal(),
            inv.getTaxRate(),
            inv.getTaxAmount(),
            inv.getDiscountAmount(),
            inv.getTotal(),
            inv.getAmountPaid(),
            inv.getAmountDue(),
            inv.getBillingPeriodStart(),
            inv.getBillingPeriodEnd(),
            inv.getIssuedAt(),
            inv.getDueDate(),
            inv.getPaidAt(),
            inv.getNotes(),
            itemResponses,
            inv.getCreatedAt(),
            inv.getUpdatedAt()
        );
    }
}

