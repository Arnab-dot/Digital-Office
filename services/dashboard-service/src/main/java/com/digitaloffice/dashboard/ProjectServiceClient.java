package com.digitaloffice.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class ProjectServiceClient {
    private final RestClient client;
    public ProjectServiceClient(@Value("${internal.project-service.url:http://localhost:8084}") String url) {
        client = RestClient.builder().baseUrl(url).build();
    }
    public List<DashboardService.ProjectInvitationSummary> getPendingInvitations(UUID actorId, String bearerToken) {
        List<Map<String, Object>> rows = client.get().uri("/api/v1/project-invitations/pending")
                .header("Authorization", bearerToken).retrieve().body(new ParameterizedTypeReference<>() {});
        if (rows == null) return List.of();
        return rows.stream().map(i -> new DashboardService.ProjectInvitationSummary(
                UUID.fromString(i.get("id").toString()), UUID.fromString(i.get("projectId").toString()))).toList();
    }
}
