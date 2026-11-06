package com.digitaloffice.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationServiceClient {
    private final RestClient client;
    public NotificationServiceClient(@Value("${internal.notification-service.url:http://localhost:8087}") String url) {
        client = RestClient.builder().baseUrl(url).build();
    }
    public int countUnreadNotifications(java.util.UUID actorId, String bearerToken) {
        JsonNode page = client.get().uri("/api/v1/notifications?readStatus=unread&size=1")
                .header("Authorization", bearerToken).retrieve().body(JsonNode.class);
        return page == null ? 0 : page.path("totalElements").asInt(0);
    }
}
