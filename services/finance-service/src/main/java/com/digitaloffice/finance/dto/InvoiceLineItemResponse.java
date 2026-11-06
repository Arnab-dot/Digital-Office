package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.InvoiceLineItem;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineItemResponse(
    UUID id,
    UUID invoiceId,
    String description,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal amount,
    int sortOrder
) {
    public static InvoiceLineItemResponse from(InvoiceLineItem item) {
        return new InvoiceLineItemResponse(
            item.getId(),
            item.getInvoiceId(),
            item.getDescription(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getAmount(),
            item.getSortOrder()
        );
    }
}

