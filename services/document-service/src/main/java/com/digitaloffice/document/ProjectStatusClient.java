package com.digitaloffice.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class ProjectStatusClient {
    private final RestClient client;
    public ProjectStatusClient(@Value("${internal.project-service.url:http://localhost:8084}") String baseUrl) {
        client = RestClient.builder().baseUrl(baseUrl).build();
    }
    public void requireOpen(UUID projectId, String bearerToken) {
        try {
            ProjectStatus status = client.get().uri("/internal/projects/{id}/status", projectId)
                    .header("Authorization", bearerToken).retrieve().body(ProjectStatus.class);
            if (status == null || "CLOSED".equals(status.status()))
                throw new IllegalStateException("Closed project cannot be modified");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Project-service is unavailable while checking project status", ex);
        }
    }
    private record ProjectStatus(String status) {}
}
