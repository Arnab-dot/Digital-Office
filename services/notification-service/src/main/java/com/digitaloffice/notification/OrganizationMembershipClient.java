package com.digitaloffice.notification;

import com.digitaloffice.contracts.MembershipSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Component
public class OrganizationMembershipClient {

    private static final ParameterizedTypeReference<List<MembershipSummary>> MEMBERSHIPS =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public OrganizationMembershipClient(
            @Value("${internal.organization-service.url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<MembershipSummary> activeMemberships(UUID actorId, String bearerToken) {
        try {
            List<MembershipSummary> result = restClient.get()
                    .uri("/internal/memberships/{actorId}", actorId)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .body(MEMBERSHIPS);
            return result == null ? List.of() : result;
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException(
                    "Organization service is unavailable; membership verification failed.", ex);
        }
    }
}

