package com.digitaloffice.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class DocumentServiceClient {
    private final RestClient client;
    public DocumentServiceClient(@Value("${internal.document-service.url:http://localhost:8086}") String url) {
        client = RestClient.builder().baseUrl(url).build();
    }
    public List<DashboardService.DocumentVersionSummary> getPendingReviews(UUID actorId, String bearerToken) {
        List<Map<String, Object>> rows = client.get().uri("/internal/reviews/pending")
                .header("Authorization", bearerToken).retrieve().body(new ParameterizedTypeReference<>() {});
        if (rows == null) return List.of();
        return rows.stream().map(v -> new DashboardService.DocumentVersionSummary(
                UUID.fromString(v.get("id").toString()), UUID.fromString(v.get("documentId").toString()))).toList();
    }
}
