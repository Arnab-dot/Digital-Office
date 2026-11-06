package com.digitaloffice.contracts;

/**
 * Minimal self-contained copy of organization membership roles used in cross-service contracts.
 *
 * <p>Mirrors {@code com.digitaloffice.organization.OrganizationRole} but lives in
 * {@code platform-contracts} so services that depend on this library do not need a
 * compile-time dependency on organization-service.
 */
public enum MembershipRole {
    OWNER,
    ADMIN,
    MEMBER,
    GUEST
}
