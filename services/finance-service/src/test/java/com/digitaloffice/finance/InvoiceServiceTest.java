package com.digitaloffice.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceLineItemRepository lineItemRepository;

    private InvoiceService service;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new InvoiceService(invoiceRepository, lineItemRepository);
    }

    @Test
    void testCreateInvoice() {
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = service.createInvoice(
                orgId, null, "USD", new BigDecimal("0.1000"),
                Instant.now(), Instant.now().plusSeconds(86400), Instant.now().plusSeconds(172800)
        );

        assertNotNull(result);
        assertEquals(orgId, result.getOrganizationId());
        assertEquals(InvoiceStatus.DRAFT, result.getStatus());
        assertTrue(result.getInvoiceNumber().startsWith("INV-"));
    }

    @Test
    void testAddLineItem_RecalculatesTotalsAndTax() {
        Invoice invoice = new Invoice(
                "INV-100", orgId, null, "USD", new BigDecimal("0.1000"),
                Instant.now(), Instant.now().plusSeconds(86400), Instant.now().plusSeconds(172800)
        );
        UUID invoiceId = invoice.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.save(any(InvoiceLineItem.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceLineItem item1 = new InvoiceLineItem(invoiceId, "User seats", 2, new BigDecimal("50.0000"), 0);
        when(lineItemRepository.findByInvoiceIdOrderBySortOrderAsc(invoiceId)).thenReturn(List.of(item1));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceLineItem added = service.addLineItem(invoiceId, "User seats", 2, new BigDecimal("50.0000"));

        assertNotNull(added);
        assertEquals(0, added.getAmount().compareTo(new BigDecimal("100.0000")));
        // Subtotal = 100, Tax = 10% of 100 = 10.00, Total = 110.00
        assertEquals(0, invoice.getSubtotal().compareTo(new BigDecimal("100.0000")));
        assertEquals(0, invoice.getTaxAmount().compareTo(new BigDecimal("10.0000")));
        assertEquals(0, invoice.getTotal().compareTo(new BigDecimal("110.0000")));
        assertEquals(0, invoice.getAmountDue().compareTo(new BigDecimal("110.0000")));
    }

    @Test
    void testIssueInvoice() {
        Invoice invoice = new Invoice(
                "INV-100", orgId, null, "USD", BigDecimal.ZERO,
                Instant.now(), Instant.now().plusSeconds(86400), Instant.now().plusSeconds(172800)
        );
        UUID invoiceId = invoice.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice issued = service.issueInvoice(invoiceId);
        assertEquals(InvoiceStatus.ISSUED, issued.getStatus());
        assertNotNull(issued.getIssuedAt());
    }

    @Test
    void testVoidInvoice() {
        Invoice invoice = new Invoice(
                "INV-100", orgId, null, "USD", BigDecimal.ZERO,
                Instant.now(), Instant.now().plusSeconds(86400), Instant.now().plusSeconds(172800)
        );
        UUID invoiceId = invoice.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice voided = service.voidInvoice(invoiceId);
        assertEquals(InvoiceStatus.VOID, voided.getStatus());
    }
}
