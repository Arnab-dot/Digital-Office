package com.digitaloffice.finance;

import com.digitaloffice.contracts.MembershipRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Centralizes organization-level authorization for every finance workflow. */
@Service
public class FinanceAuthorizationService {

    private final OrganizationMembershipClient memberships;

    public FinanceAuthorizationService(OrganizationMembershipClient memberships) {
        this.memberships = memberships;
    }

    public void requireFinanceAdministrator(UUID organizationId, UUID actorId, String bearerToken) {
        boolean permitted = memberships.activeMemberships(actorId, bearerToken).stream()
                .anyMatch(membership -> organizationId.equals(membership.organizationId())
                        && (membership.role() == MembershipRole.OWNER
                        || membership.role() == MembershipRole.ADMIN));
        if (!permitted) {
            throw new AccessDeniedException("Organization owner or administrator permission is required for finance access");
        }
    }

    public void requireMember(UUID organizationId, UUID actorId, String bearerToken) {
        boolean permitted = memberships.activeMemberships(actorId, bearerToken).stream()
                .anyMatch(membership -> organizationId.equals(membership.organizationId()));
        if (!permitted) {
            throw new AccessDeniedException("Organization access denied");
        }
    }
}
