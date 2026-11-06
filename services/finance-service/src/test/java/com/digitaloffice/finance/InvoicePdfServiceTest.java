package com.digitaloffice.finance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvoicePdfServiceTest {

    @Test
    void testRenderPdf_ProducesValidPdfBytes() {
        InvoicePdfService service = new InvoicePdfService();

        UUID orgId = UUID.randomUUID();
        Invoice invoice = new Invoice(
                "INV-202610-001", orgId, null, "USD", new BigDecimal("0.1000"),
                Instant.now(), Instant.now().plusSeconds(86400), Instant.now().plusSeconds(172800)
        );
        invoice.recalculate(new BigDecimal("200.00"), BigDecimal.ZERO);

        InvoiceLineItem item1 = new InvoiceLineItem(
                invoice.getId(), "Starter Subscription", 1, new BigDecimal("200.00"), 0);

        byte[] pdfBytes = service.render(invoice, List.of(item1));

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);

        String pdfString = new String(pdfBytes, StandardCharsets.ISO_8859_1);
        assertTrue(pdfString.startsWith("%PDF-1.4"));
        assertTrue(pdfString.contains("INV-202610-001"));
        assertTrue(pdfString.contains("Starter Subscription"));
        assertTrue(pdfString.contains("%%EOF"));
    }
}

