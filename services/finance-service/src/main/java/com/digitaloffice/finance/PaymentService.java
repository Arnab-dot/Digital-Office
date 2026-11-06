package com.digitaloffice.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          RefundRepository refundRepository,
                          InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public Payment recordPayment(UUID organizationId, UUID invoiceId, PaymentMethod method,
                                  BigDecimal amount, String currency, String referenceNumber,
                                  String idempotencyKey, String notes) {
        // Idempotency check
        if (idempotencyKey != null) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get(); // return existing payment for idempotent requests
            }
        }

        Payment payment = new Payment(organizationId, invoiceId, method, amount,
                currency, referenceNumber, idempotencyKey, notes);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment confirmPayment(UUID paymentId, UUID confirmedBy) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payments can be confirmed");
        }
        payment.confirm(confirmedBy);
        payment = paymentRepository.save(payment);

        // Apply payment to invoice
        if (payment.getInvoiceId() != null) {
            Invoice invoice = invoiceRepository.findById(payment.getInvoiceId()).orElse(null);
            if (invoice != null) {
                invoice.recordPayment(payment.getAmount());
                invoiceRepository.save(invoice);
            }
        }

        return payment;
    }

    @Transactional
    public Payment failPayment(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payments can be marked as failed");
        }
        payment.fail();
        return paymentRepository.save(payment);
    }

    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found"));
    }

    public Page<Payment> listPayments(UUID organizationId, int page, int size) {
        int bounded = Math.min(Math.max(size, 1), 50);
        return paymentRepository.findByOrganizationId(organizationId,
                PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional
    public Refund requestRefund(UUID paymentId, UUID organizationId, BigDecimal amount, String reason) {
        Payment payment = getPayment(paymentId);
        if (!payment.getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("Payment does not belong to this organization");
        }
        if (payment.getStatus() != PaymentStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED payments can be refunded");
        }
        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }
        return refundRepository.save(new Refund(paymentId, organizationId, amount, reason));
    }

    @Transactional
    public Refund processRefund(UUID refundId, UUID processedBy) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NoSuchElementException("Refund not found"));
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException("Only PENDING refunds can be processed");
        }
        refund.process(processedBy);
        return refundRepository.save(refund);
    }

    public List<Refund> listRefunds(UUID organizationId) {
        return refundRepository.findByOrganizationId(organizationId);
    }
}
