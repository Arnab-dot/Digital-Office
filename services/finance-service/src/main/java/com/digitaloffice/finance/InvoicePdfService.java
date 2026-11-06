package com.digitaloffice.finance;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces a compact, dependency-free PDF representation of an invoice.  The
 * generated document is intentionally plain: the database remains the source
 * of truth and this renderer never stores a mutable invoice copy.
 */
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    public byte[] render(Invoice invoice, List<InvoiceLineItem> lineItems) {
        List<String> lines = new ArrayList<>();
        lines.add("Digital Office OS");
        lines.add("Invoice " + invoice.getInvoiceNumber());
        lines.add("Status: " + invoice.getStatus());
        lines.add("Billing period: " + date(invoice.getBillingPeriodStart()) + " to " + date(invoice.getBillingPeriodEnd()));
        lines.add("Due date: " + date(invoice.getDueDate()));
        lines.add("");
        for (InvoiceLineItem item : lineItems) {
            lines.add(item.getDescription() + "  x" + item.getQuantity() + "  "
                    + money(item.getAmount(), invoice.getCurrency()));
        }
        lines.add("");
        lines.add("Subtotal: " + money(invoice.getSubtotal(), invoice.getCurrency()));
        lines.add("Discount: " + money(invoice.getDiscountAmount(), invoice.getCurrency()));
        lines.add("Tax: " + money(invoice.getTaxAmount(), invoice.getCurrency()));
        lines.add("Total: " + money(invoice.getTotal(), invoice.getCurrency()));
        lines.add("Amount paid: " + money(invoice.getAmountPaid(), invoice.getCurrency()));
        lines.add("Amount due: " + money(invoice.getAmountDue(), invoice.getCurrency()));
        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            lines.add("");
            lines.add("Notes: " + invoice.getNotes());
        }

        StringBuilder content = new StringBuilder("BT\n/F1 12 Tf\n50 780 Td\n");
        boolean first = true;
        for (String line : lines) {
            if (!first) {
                content.append("0 -18 Td\n");
            }
            content.append('(').append(escape(line)).append(") Tj\n");
            first = false;
        }
        content.append("ET\n");
        byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + stream.length + " >>\nstream\n" + new String(stream, StandardCharsets.ISO_8859_1) + "endstream"
        );
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.length());
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.size(); i++) {
            pdf.append(String.format("%010d 00000 n \n", offsets.get(i)));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1)
                .append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String date(java.time.Instant value) {
        return value == null ? "—" : DATE.format(value);
    }

    private static String money(BigDecimal value, String currency) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " " + currency;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}
