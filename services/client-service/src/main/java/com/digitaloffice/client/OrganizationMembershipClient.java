package com.digitaloffice.client;

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
 *
 * <p>Calls {@code GET /internal/memberships/{userId}} with the actor's bearer token
 * forwarded as the Authorization header.
 *
 * <p>On any {@link RestClientException} (connection failure, timeout, etc.) a
 * {@link ServiceUnavailableException} is thrown so the caller receives HTTP 503,
 * satisfying Requirement 1.11 (fail-closed behavior).
 */
@Component
public class OrganizationMembershipClient {

    private static final ParameterizedTypeReference<List<MembershipSummary>> MEMBERSHIP_LIST =
            new ParameterizedTypeReference<>() {};

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
     * @param actorId     internal UUID derived from the JWT sub claim
     * @param bearerToken the raw bearer token (e.g. {@code "Bearer eyJ..."})
     * @return list of active {@link MembershipSummary} records; never null
     * @throws ServiceUnavailableException if Organization_Service is unreachable
     */
    public List<MembershipSummary> getActiveMemberships(UUID actorId, String bearerToken) {
        try {
            List<MembershipSummary> result = restClient.get()
                    .uri("/internal/memberships/{userId}", actorId)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .body(MEMBERSHIP_LIST);
            return result != null ? result : List.of();
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException(
                    "Organization service is unavailable. Please try again later.", ex);
        }
    }

    /**
     * Returns {@code true} if the actor holds an ACTIVE membership in the given organization.
     *
     * @param actorId     internal UUID derived from the JWT sub claim
     * @param orgId       the organization to check membership for
     * @param bearerToken the raw bearer token (e.g. {@code "Bearer eyJ..."})
     * @return {@code true} if an active membership exists; {@code false} otherwise
     * @throws ServiceUnavailableException if Organization_Service is unreachable
     */
    public boolean hasActiveMembership(UUID actorId, UUID orgId, String bearerToken) {
        return getActiveMemberships(actorId, bearerToken).stream()
                .anyMatch(m -> orgId.equals(m.organizationId()));
    }
}
