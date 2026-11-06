package com.digitaloffice.work;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class DocumentActivityClient {
    private final RestClient client;
    public DocumentActivityClient(@Value("${internal.document-service.url:http://localhost:8086}") String baseUrl) {
        client = RestClient.builder().baseUrl(baseUrl).build();
    }
    public List<ActivityFeedItem> forProject(UUID projectId, String token) {
        try {
            List<DocumentEvent> events = client.get().uri("/internal/activity?projectId={id}", projectId)
                    .header("Authorization", token).retrieve().body(new ParameterizedTypeReference<>() {});
            if (events == null) return List.of();
            return events.stream().map(e -> new ActivityFeedItem(e.id(), e.projectId(), e.actorId(), e.actionType(),
                    e.subjectId(), e.subjectType(), e.metadata(), e.eventAt())).toList();
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Document-service is unavailable while loading project activity", ex);
        }
    }
    private record DocumentEvent(UUID id, UUID projectId, UUID actorId, String actionType,
                                 UUID subjectId, String subjectType, String metadata, Instant eventAt) {}
}
