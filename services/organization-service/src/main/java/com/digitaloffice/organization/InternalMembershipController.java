package com.digitaloffice.organization;

import com.digitaloffice.contracts.MembershipRole;
import com.digitaloffice.contracts.MembershipSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Internal endpoint used by other microservices to resolve the active organization
 * memberships of a given user. Not exposed via the external API Gateway — only
 * reachable from within the service mesh.
 */
@RestController
@RequestMapping("/internal")
public class InternalMembershipController {

    private final MembershipRepository membershipRepository;

    public InternalMembershipController(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    /**
     * Returns the list of ACTIVE organization memberships for the given user.
     *
     * <p>Requirement 1.6 — membership verification is performed exclusively by
     * Organization_Service using the actor's internal UUID.
     *
     * @param userId the actor's internal UUID (derived from JWT {@code sub} claim by callers)
     * @return list of {@link MembershipSummary} records, one per ACTIVE membership
     */
    @GetMapping("/memberships/{userId}")
    public List<MembershipSummary> getActiveMemberships(@PathVariable UUID userId) {
        return membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)
                .stream()
                .map(m -> new MembershipSummary(m.getOrganizationId(), toMembershipRole(m.getRole())))
                .toList();
    }

    /**
     * Maps an organization-service {@link OrganizationRole} to the contracts library
     * {@link MembershipRole}, keeping the contracts library free of a compile dependency
     * on organization-service.
     */
    private MembershipRole toMembershipRole(OrganizationRole role) {
        return switch (role) {
            case OWNER  -> MembershipRole.OWNER;
            case ADMIN  -> MembershipRole.ADMIN;
            case MEMBER -> MembershipRole.MEMBER;
            case GUEST  -> MembershipRole.GUEST;
        };
    }
}
