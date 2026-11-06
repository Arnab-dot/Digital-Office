package com.digitaloffice.project;

import com.digitaloffice.contracts.MembershipSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

/**
 * HTTP client for querying active organization memberships from Organization_Service.
 * Calls the internal endpoint {@code GET /internal/memberships/{userId}}.
 * Fails closed with HTTP 503 ({@link ServiceUnavailableException}) on connection errors.
 */
@Component
public class OrganizationMembershipClient {

    private final RestClient restClient;

    public OrganizationMembershipClient(
            @Value("${internal.organization-service.url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Returns all ACTIVE memberships for the given actor.
     *
     * @param actorId     the actor's internal UUID
     * @param bearerToken the JWT bearer token to forward for authentication
     * @return list of active membership summaries
     * @throws ServiceUnavailableException if Organization_Service is unreachable
     */
    public List<MembershipSummary> getActiveMemberships(UUID actorId, String bearerToken) {
        try {
            return restClient.get()
                    .uri("/internal/memberships/{userId}", actorId)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MembershipSummary>>() {});
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException(
                    "Organization Service is unavailable: " + ex.getMessage(), ex);
        }
    }

    /**
     * Returns {@code true} if the actor has an ACTIVE membership in the given organization.
     *
     * @param actorId     the actor's internal UUID
     * @param orgId       the organization UUID to check
     * @param bearerToken the JWT bearer token to forward for authentication
     * @return {@code true} if an active membership exists, {@code false} otherwise
     * @throws ServiceUnavailableException if Organization_Service is unreachable
     */
    public boolean hasActiveMembership(UUID actorId, UUID orgId, String bearerToken) {
        List<MembershipSummary> memberships = getActiveMemberships(actorId, bearerToken);
        return memberships != null && memberships.stream()
                .anyMatch(m -> orgId.equals(m.organizationId()));
    }
}
