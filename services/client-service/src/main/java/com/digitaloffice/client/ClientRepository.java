package com.digitaloffice.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link Client} entities.
 *
 * <p>The former {@code findByOrganizationIdAndCreatedByAndStatus} method has been replaced
 * by {@link #findByOrganizationIdAndStatus} — the {@code createdBy} filter is no longer
 * used now that membership-based access control governs visibility (Requirement 1.7).
 */
public interface ClientRepository extends JpaRepository<Client, UUID> {

    /**
     * Returns all clients belonging to the given organization with the given status.
     * Used by {@code ClientService.list()} after the org-membership gate has been passed.
     *
     * @param organizationId the owning organization UUID
     * @param status         the desired client status (typically {@link ClientStatus#ACTIVE})
     * @return matching clients
     */
    List<Client> findByOrganizationIdAndStatus(UUID organizationId, ClientStatus status);
}
