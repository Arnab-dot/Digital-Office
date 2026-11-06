package com.digitaloffice.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InvoiceService {

    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100000);
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoiceLineItemRepository lineItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.lineItemRepository = lineItemRepository;
    }

    @Transactional
    public Invoice createInvoice(UUID organizationId, UUID subscriptionId, String currency,
                                  BigDecimal taxRate, Instant billingPeriodStart,
                                  Instant billingPeriodEnd, Instant dueDate) {
        String invoiceNumber = generateInvoiceNumber();
        Invoice invoice = new Invoice(invoiceNumber, organizationId, subscriptionId,
                currency, taxRate, billingPeriodStart, billingPeriodEnd, dueDate);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public InvoiceLineItem addLineItem(UUID invoiceId, String description, int quantity,
                                        BigDecimal unitPrice) {
        Invoice invoice = getInvoice(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Can only add line items to DRAFT invoices");
        }

        List<InvoiceLineItem> existingItems = lineItemRepository.findByInvoiceIdOrderBySortOrderAsc(invoiceId);
        int nextOrder = existingItems.size();

        InvoiceLineItem item = new InvoiceLineItem(invoiceId, description, quantity, unitPrice, nextOrder);
        item = lineItemRepository.save(item);

        recalculateInvoice(invoice);
        return item;
    }

    @Transactional
    public Invoice issueInvoice(UUID invoiceId) {
        Invoice invoice = getInvoice(invoiceId);
        invoice.issue();
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice voidInvoice(UUID invoiceId) {
        Invoice invoice = getInvoice(invoiceId);
        invoice.voidInvoice();
        return invoiceRepository.save(invoice);
    }

    public Invoice getInvoice(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found"));
    }

    public Page<Invoice> listInvoices(UUID organizationId, int page, int size) {
        int bounded = Math.min(Math.max(size, 1), 50);
        return invoiceRepository.findByOrganizationId(organizationId,
                PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public List<InvoiceLineItem> getLineItems(UUID invoiceId) {
        return lineItemRepository.findByInvoiceIdOrderBySortOrderAsc(invoiceId);
    }

    private void recalculateInvoice(Invoice invoice) {
        List<InvoiceLineItem> items = lineItemRepository.findByInvoiceIdOrderBySortOrderAsc(invoice.getId());
        BigDecimal subtotal = items.stream()
                .map(InvoiceLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.recalculate(subtotal, invoice.getDiscountAmount());
        invoiceRepository.save(invoice);
    }

    private String generateInvoiceNumber() {
        String datePart = DateTimeFormatter.ofPattern("yyyyMM")
                .format(Instant.now().atOffset(ZoneOffset.UTC));
        long seq = SEQUENCE.incrementAndGet();
        return "INV-" + datePart + "-" + String.format("%05d", seq % 100000);
    }
}
