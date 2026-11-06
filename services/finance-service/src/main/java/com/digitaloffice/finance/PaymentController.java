package com.digitaloffice.finance;

import com.digitaloffice.finance.dto.PaymentResponse;
import com.digitaloffice.finance.dto.RecordPaymentRequest;
import com.digitaloffice.finance.dto.RefundRequest;
import com.digitaloffice.finance.dto.RefundResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final FinanceAuthorizationService authService;

    public PaymentController(PaymentService paymentService,
                             FinanceAuthorizationService authService) {
        this.paymentService = paymentService;
        this.authService = authService;
    }

    @GetMapping
    public Page<PaymentResponse> listPayments(@PathVariable UUID organizationId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @AuthenticationPrincipal Jwt jwt,
                                              HttpServletRequest request) {
        authService.requireMember(organizationId, actor(jwt), token(request));
        return paymentService.listPayments(organizationId, page, size)
                .map(PaymentResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse recordPayment(@PathVariable UUID organizationId,
                                         @Valid @RequestBody RecordPaymentRequest body,
                                         @AuthenticationPrincipal Jwt jwt,
                                         HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));

        String currency = body.currency() != null ? body.currency() : "USD";
        Payment payment = paymentService.recordPayment(
                organizationId, body.invoiceId(), body.paymentMethod(),
                body.amount(), currency, body.referenceNumber(),
                body.idempotencyKey(), body.notes()
        );

        return PaymentResponse.from(payment);
    }

    @PostMapping("/{paymentId}/confirm")
    public PaymentResponse confirmPayment(@PathVariable UUID organizationId,
                                          @PathVariable UUID paymentId,
                                          @AuthenticationPrincipal Jwt jwt,
                                          HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        Payment payment = paymentService.getPayment(paymentId);
        validateOrgOwnership(payment, organizationId);

        payment = paymentService.confirmPayment(paymentId, actor(jwt));
        return PaymentResponse.from(payment);
    }

    @PostMapping("/{paymentId}/fail")
    public PaymentResponse failPayment(@PathVariable UUID organizationId,
                                       @PathVariable UUID paymentId,
                                       @AuthenticationPrincipal Jwt jwt,
                                       HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        Payment payment = paymentService.getPayment(paymentId);
        validateOrgOwnership(payment, organizationId);

        payment = paymentService.failPayment(paymentId);
        return PaymentResponse.from(payment);
    }

    @PostMapping("/{paymentId}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse requestRefund(@PathVariable UUID organizationId,
                                        @PathVariable UUID paymentId,
                                        @Valid @RequestBody RefundRequest body,
                                        @AuthenticationPrincipal Jwt jwt,
                                        HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        Payment payment = paymentService.getPayment(paymentId);
        validateOrgOwnership(payment, organizationId);

        Refund refund = paymentService.requestRefund(paymentId, organizationId, body.amount(), body.reason());
        return RefundResponse.from(refund);
    }

    private void validateOrgOwnership(Payment payment, UUID organizationId) {
        if (!payment.getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("Payment does not belong to organization");
        }
    }

    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
    }

    private static String token(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }
}

