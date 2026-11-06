package com.digitaloffice.notification;

import com.digitaloffice.contracts.MembershipRole;
import com.digitaloffice.contracts.MembershipSummary;
import com.digitaloffice.notification.dto.AuditLogResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/audit-logs")
public class AuditController {

    private final AuditLogService service;
    private final OrganizationMembershipClient membershipClient;

    public AuditController(AuditLogService service, OrganizationMembershipClient membershipClient) {
        this.service = service;
        this.membershipClient = membershipClient;
    }

    @GetMapping
    public Page<AuditLogResponse> getAuditLogs(@PathVariable UUID organizationId,
                                              @RequestParam(required = false) String action,
                                              @RequestParam(required = false) UUID actorId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @AuthenticationPrincipal Jwt jwt,
                                              HttpServletRequest request) {
        UUID actor = actor(jwt);
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Security check: Must be OWNER or ADMIN of the organization
        boolean authorized = false;
        try {
            List<MembershipSummary> memberships = membershipClient.activeMemberships(actor, token);
            authorized = memberships.stream().anyMatch(m ->
                    organizationId.equals(m.organizationId())
                            && (m.role() == MembershipRole.OWNER || m.role() == MembershipRole.ADMIN));
        } catch (Exception ignored) {
            // If membership check fails, access denied
        }

        if (!authorized) {
            throw new AccessDeniedException("Organization owner or administrator permission is required to view audit logs");
        }

        return service.getOrganizationAuditLogs(organizationId, action, actorId, page, size)
                .map(AuditLogResponse::from);
    }

    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
    }
}

