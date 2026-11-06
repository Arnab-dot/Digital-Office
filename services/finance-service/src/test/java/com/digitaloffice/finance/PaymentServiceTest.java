package com.digitaloffice.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private InvoiceRepository invoiceRepository;

    private PaymentService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, refundRepository, invoiceRepository);
    }

    @Test
    void testRecordPayment_Success() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = service.recordPayment(
                orgId, invoiceId, PaymentMethod.BANK_TRANSFER, new BigDecimal("100.00"),
                "USD", "REF-999", "idempotency-123", "Payment notes"
        );

        assertNotNull(payment);
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(orgId, payment.getOrganizationId());
        assertEquals("idempotency-123", payment.getIdempotencyKey());
    }

    @Test
    void testRecordPayment_Idempotent() {
        Payment existing = new Payment(
                orgId, invoiceId, PaymentMethod.BANK_TRANSFER, new BigDecimal("100.00"),
                "USD", "REF-999", "idempotency-123", "Payment notes"
        );
        when(paymentRepository.findByIdempotencyKey("idempotency-123")).thenReturn(Optional.of(existing));

        Payment payment = service.recordPayment(
                orgId, invoiceId, PaymentMethod.BANK_TRANSFER, new BigDecimal("100.00"),
                "USD", "REF-999", "idempotency-123", "Payment notes"
        );

        assertSame(existing, payment);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testConfirmPayment_UpdatesInvoice() {
        Payment payment = new Payment(
                orgId, invoiceId, PaymentMethod.BANK_TRANSFER, new BigDecimal("100.00"),
                "USD", "REF-999", "idempotency-123", "Payment notes"
        );
        UUID paymentId = payment.getId();

        Invoice invoice = new Invoice(
                "INV-100", orgId, null, "USD", BigDecimal.ZERO,
                Instant.now(), Instant.now().plusSeconds(86400), Instant.now().plusSeconds(172800)
        );
        invoice.recalculate(new BigDecimal("100.00"), BigDecimal.ZERO); // total = 100, due = 100
        invoice.issue();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment confirmed = service.confirmPayment(paymentId, actorId);

        assertEquals(PaymentStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(actorId, confirmed.getConfirmedBy());
        assertEquals(0, invoice.getAmountPaid().compareTo(new BigDecimal("100.00")));
        assertEquals(0, invoice.getAmountDue().compareTo(BigDecimal.ZERO));
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
    }

    @Test
    void testFailPayment() {
        Payment payment = new Payment(
                orgId, invoiceId, PaymentMethod.BANK_TRANSFER, new BigDecimal("100.00"),
                "USD", "REF-999", "idempotency-123", "Payment notes"
        );
        UUID paymentId = payment.getId();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment failed = service.failPayment(paymentId);
        assertEquals(PaymentStatus.FAILED, failed.getStatus());
    }

    @Test
    void testRefundPayment() {
        Payment payment = new Payment(
                orgId, invoiceId, PaymentMethod.BANK_TRANSFER, new BigDecimal("100.00"),
                "USD", "REF-999", "idempotency-123", "Payment notes"
        );
        UUID paymentId = payment.getId();
        payment.confirm(actorId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        Refund refund = service.requestRefund(paymentId, orgId, new BigDecimal("50.00"), "Overpayment");

        assertNotNull(refund);
        assertEquals(new BigDecimal("50.00"), refund.getAmount());
        assertEquals(RefundStatus.PENDING, refund.getStatus());
    }
}
