package com.digitaloffice.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {
    List<InvoiceLineItem> findByInvoiceIdOrderBySortOrderAsc(UUID invoiceId);
    void deleteByInvoiceId(UUID invoiceId);
}
