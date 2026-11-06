package com.digitaloffice.client;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Business logic for client management.
 *
 * <p>Every mutating and read operation verifies that the actor holds an ACTIVE
 * organization membership (via {@link OrganizationMembershipClient}) before any
 * data is touched, satisfying Requirements 1.1, 1.5, 1.7, and 12.1.
 *
 * <p>Opaque 403/404 policy:
 * <ul>
 *   <li>Actor is not an active member of the relevant org → {@link NoSuchElementException}
 *       (→ HTTP 404) — prevents resource enumeration.</li>
 *   <li>Actor is a member of the org but targets a client that belongs to a
 *       <em>different</em> org → {@link AccessDeniedException} (→ HTTP 403).</li>
 * </ul>
 */
@Service
public class ClientService {

    private final ClientRepository clients;
    private final OrganizationMembershipClient membershipClient;

    public ClientService(ClientRepository clients, OrganizationMembershipClient membershipClient) {
        this.clients = clients;
        this.membershipClient = membershipClient;
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    /**
     * Creates a new client in {@code orgId} if the actor is an ACTIVE member.
     *
     * @param orgId       organization that will own the client
     * @param actor       internal UUID of the authenticated user
     * @param request     validated client fields
     * @param bearerToken forwarded JWT bearer token
     * @return the persisted {@link Client}
     * @throws NoSuchElementException      if the actor is not an ACTIVE member of {@code orgId}
     * @throws ServiceUnavailableException if Organization_Service is unreachable
     */
    public Client create(UUID orgId, UUID actor, ClientController.ClientRequest request,
                         String bearerToken) {
        requireMembership(actor, orgId, bearerToken);
        return clients.save(new Client(orgId, request.name(), request.description(),
                request.contactEmail(), actor));
    }

    // -------------------------------------------------------------------------
    // list
    // -------------------------------------------------------------------------

    /**
     * Returns all ACTIVE clients in {@code orgId}.
     *
     * <p>Replaces the previous {@code createdBy}-scoped filter with an org-membership
     * gate per Requirement 1.7: all members of the org may see all clients in that org.
     *
     * @param orgId       organization to list clients for
     * @param actor       internal UUID of the authenticated user
     * @param bearerToken forwarded JWT bearer token
     * @return list of ACTIVE clients in the org
     * @throws NoSuchElementException      if the actor is not an ACTIVE member of {@code orgId}
     * @throws ServiceUnavailableException if Organization_Service is unreachable
     */
    public List<Client> list(UUID orgId, UUID actor, String bearerToken) {
        requireMembership(actor, orgId, bearerToken);
        return clients.findByOrganizationIdAndStatus(orgId, ClientStatus.ACTIVE);
    }

    // -------------------------------------------------------------------------
    // get
    // -------------------------------------------------------------------------

    /**
     * Returns the client identified by {@code id}, provided the actor is an
     * ACTIVE member of the client's organization.
     *
     * @param id          client UUID
     * @param actor       internal UUID of the authenticated user
     * @param bearerToken forwarded JWT bearer token
     * @return the requested {@link Client}
     * @throws NoSuchElementException      if the client does not exist, or if the actor
     *                                     is not a member of the client's org (opaque)
     * @throws AccessDeniedException       if the actor is a member of the client's org but the
     *                                     resource is outside their granted scope
     * @throws ServiceUnavailableException if Organization_Service is unreachable
     */
    public Client get(UUID id, UUID actor, String bearerToken) {
        Client client = clients.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found"));
        requireMembership(actor, client.getOrganizationId(), bearerToken);
        return client;
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    /**
     * Updates the client identified by {@code id}, provided the actor is an
     * ACTIVE member of the client's organization.
     *
     * @param id          client UUID
     * @param actor       internal UUID of the authenticated user
     * @param request     validated updated fields
     * @param bearerToken forwarded JWT bearer token
     * @return the updated and persisted {@link Client}
     * @throws NoSuchElementException      if the client does not exist or actor is not a member
     * @throws ServiceUnavailableException if Organization_Service is unreachable
     */
    public Client update(UUID id, UUID actor, ClientController.ClientRequest request,
                         String bearerToken) {
        Client client = get(id, actor, bearerToken);
        client.update(request.name(), request.description(), request.contactEmail());
        return clients.save(client);
    }

    // -------------------------------------------------------------------------
    // archive
    // -------------------------------------------------------------------------

    /**
     * Archives the client identified by {@code id}, provided the actor is an
     * ACTIVE member of the client's organization.
     *
     * @param id          client UUID
     * @param actor       internal UUID of the authenticated user
     * @param bearerToken forwarded JWT bearer token
     * @throws NoSuchElementException      if the client does not exist or actor is not a member
     * @throws ServiceUnavailableException if Organization_Service is unreachable
     */
    public void archive(UUID id, UUID actor, String bearerToken) {
        Client client = get(id, actor, bearerToken);
        client.archive();
        clients.save(client);
    }

    // -------------------------------------------------------------------------
    // internal helpers
    // -------------------------------------------------------------------------

    /**
     * Verifies the actor has an ACTIVE membership in {@code orgId}.
     *
     * <p>Returns silently on success. On failure, throws a {@link NoSuchElementException}
     * (HTTP 404) to prevent resource-existence enumeration (Requirement 1.5, 12.1).
     */
    private void requireMembership(UUID actor, UUID orgId, String bearerToken) {
        if (!membershipClient.hasActiveMembership(actor, orgId, bearerToken)) {
            throw new NoSuchElementException("Resource not found or access denied");
        }
    }
}
