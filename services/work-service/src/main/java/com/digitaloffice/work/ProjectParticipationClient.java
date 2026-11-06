package com.digitaloffice.work;

import com.digitaloffice.contracts.ParticipationSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

/**
 * Internal HTTP client for querying active project participations from Project_Service.
 * Fail-closed: any {@link RestClientException} (connection failure, timeout, 5xx) is
 * re-thrown as {@link ServiceUnavailableException} which maps to HTTP 503.
 */
@Component
public class ProjectParticipationClient {

    private final RestClient restClient;

    public ProjectParticipationClient(
            @Value("${internal.project-service.url:http://localhost:8084}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Returns all ACTIVE participations for the given project.
     *
     * @param projectId   the project whose participations to fetch
     * @param bearerToken the caller's JWT bearer token (forwarded for auth on the internal endpoint)
     * @return list of {@link ParticipationSummary} records
     * @throws ServiceUnavailableException if Project_Service is unreachable
     */
    public List<ParticipationSummary> getActiveParticipations(UUID projectId, String bearerToken) {
        try {
            return restClient.get()
                    .uri("/internal/participations?projectId={id}", projectId)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ParticipationSummary>>() {});
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException(
                    "Project-service is unavailable while fetching participations for project " + projectId, ex);
        }
    }

    /**
     * Returns {@code true} if the given organization holds an ACTIVE participation in the project.
     *
     * @param projectId   the project to check
     * @param orgId       the organization to look for
     * @param bearerToken the caller's JWT bearer token
     * @return {@code true} when an active participation exists for {@code orgId}
     * @throws ServiceUnavailableException if Project_Service is unreachable
     */
    public boolean hasActiveParticipation(UUID projectId, UUID orgId, String bearerToken) {
        return getActiveParticipations(projectId, bearerToken).stream()
                .anyMatch(p -> orgId.equals(p.organizationId()));
    }

    public List<UUID> getActiveProjectIds(List<UUID> organizationIds, String bearerToken) {
        if (organizationIds == null || organizationIds.isEmpty()) return List.of();
        try {
            return restClient.get().uri(uri -> uri.path("/internal/participations/projects")
                            .queryParam("organizationIds", organizationIds.stream().map(UUID::toString).toArray()).build())
                    .header("Authorization", bearerToken).retrieve()
                    .body(new ParameterizedTypeReference<List<UUID>>() {});
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Project-service is unavailable while fetching active projects", ex);
        }
    }
}
