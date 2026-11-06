package com.digitaloffice.contracts;

import java.util.UUID;

/**
 * Projection of an active organization membership returned by Organization_Service's
 * internal endpoint {@code GET /internal/memberships/{userId}}.
 *
 * @param organizationId the organization this membership belongs to
 * @param role           the member's role within that organization
 */
public record MembershipSummary(UUID organizationId, MembershipRole role) {}
