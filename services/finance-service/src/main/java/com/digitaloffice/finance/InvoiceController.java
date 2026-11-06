package com.digitaloffice.finance;

import com.digitaloffice.finance.dto.CreateInvoiceRequest;
import com.digitaloffice.finance.dto.CreateLineItemRequest;
import com.digitaloffice.finance.dto.InvoiceLineItemResponse;
import com.digitaloffice.finance.dto.InvoiceResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService pdfService;
    private final FinanceAuthorizationService authService;

    public InvoiceController(InvoiceService invoiceService,
                             InvoicePdfService pdfService,
                             FinanceAuthorizationService authService) {
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
        this.authService = authService;
    }

    @GetMapping
    public Page<InvoiceResponse> listInvoices(@PathVariable UUID organizationId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @AuthenticationPrincipal Jwt jwt,
                                             HttpServletRequest request) {
        authService.requireMember(organizationId, actor(jwt), token(request));
        return invoiceService.listInvoices(organizationId, page, size)
                .map(inv -> InvoiceResponse.from(inv, invoiceService.getLineItems(inv.getId())));
    }

    @GetMapping("/{invoiceId}")
    public InvoiceResponse getInvoice(@PathVariable UUID organizationId,
                                      @PathVariable UUID invoiceId,
                                      @AuthenticationPrincipal Jwt jwt,
                                      HttpServletRequest request) {
        authService.requireMember(organizationId, actor(jwt), token(request));
        Invoice inv = invoiceService.getInvoice(invoiceId);
        validateOrgOwnership(inv, organizationId);
        List<InvoiceLineItem> items = invoiceService.getLineItems(invoiceId);
        return InvoiceResponse.from(inv, items);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@PathVariable UUID organizationId,
                                         @Valid @RequestBody CreateInvoiceRequest body,
                                         @AuthenticationPrincipal Jwt jwt,
                                         HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));

        BigDecimal taxRate = body.taxRate() != null ? body.taxRate() : BigDecimal.ZERO;
        String currency = body.currency() != null ? body.currency() : "USD";

        Invoice invoice = invoiceService.createInvoice(
                organizationId, body.subscriptionId(), currency, taxRate,
                body.billingPeriodStart(), body.billingPeriodEnd(), body.dueDate()
        );

        if (body.items() != null && !body.items().isEmpty()) {
            for (CreateLineItemRequest item : body.items()) {
                invoiceService.addLineItem(invoice.getId(), item.description(), item.quantity(), item.unitPrice());
            }
            invoice = invoiceService.getInvoice(invoice.getId());
        }

        List<InvoiceLineItem> items = invoiceService.getLineItems(invoice.getId());
        return InvoiceResponse.from(invoice, items);
    }

    @PostMapping("/{invoiceId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceLineItemResponse addLineItem(@PathVariable UUID organizationId,
                                               @PathVariable UUID invoiceId,
                                               @Valid @RequestBody CreateLineItemRequest body,
                                               @AuthenticationPrincipal Jwt jwt,
                                               HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        Invoice inv = invoiceService.getInvoice(invoiceId);
        validateOrgOwnership(inv, organizationId);

        InvoiceLineItem item = invoiceService.addLineItem(invoiceId, body.description(), body.quantity(), body.unitPrice());
        return InvoiceLineItemResponse.from(item);
    }

    @PostMapping("/{invoiceId}/issue")
    public InvoiceResponse issueInvoice(@PathVariable UUID organizationId,
                                        @PathVariable UUID invoiceId,
                                        @AuthenticationPrincipal Jwt jwt,
                                        HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        Invoice inv = invoiceService.getInvoice(invoiceId);
        validateOrgOwnership(inv, organizationId);

        inv = invoiceService.issueInvoice(invoiceId);
        return InvoiceResponse.from(inv, invoiceService.getLineItems(invoiceId));
    }

    @PostMapping("/{invoiceId}/void")
    public InvoiceResponse voidInvoice(@PathVariable UUID organizationId,
                                       @PathVariable UUID invoiceId,
                                       @AuthenticationPrincipal Jwt jwt,
                                       HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        Invoice inv = invoiceService.getInvoice(invoiceId);
        validateOrgOwnership(inv, organizationId);

        inv = invoiceService.voidInvoice(invoiceId);
        return InvoiceResponse.from(inv, invoiceService.getLineItems(invoiceId));
    }

    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID organizationId,
                                              @PathVariable UUID invoiceId,
                                              @AuthenticationPrincipal Jwt jwt,
                                              HttpServletRequest request) {
        authService.requireMember(organizationId, actor(jwt), token(request));
        Invoice inv = invoiceService.getInvoice(invoiceId);
        validateOrgOwnership(inv, organizationId);

        List<InvoiceLineItem> items = invoiceService.getLineItems(invoiceId);
        byte[] pdfBytes = pdfService.render(inv, items);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"invoice-" + inv.getInvoiceNumber() + ".pdf\"")
                .body(pdfBytes);
    }

    private void validateOrgOwnership(Invoice invoice, UUID organizationId) {
        if (!invoice.getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("Invoice does not belong to organization");
        }
    }

    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
    }

    private static String token(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }
}

