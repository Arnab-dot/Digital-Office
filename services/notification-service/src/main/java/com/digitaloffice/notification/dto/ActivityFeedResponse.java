package com.digitaloffice.notification.dto;

import com.digitaloffice.notification.ActivityFeedItem;
import java.time.Instant;
import java.util.UUID;

public record ActivityFeedResponse(
    UUID id,
    UUID organizationId,
    UUID projectId,
    UUID actorId,
    String category,
    String eventType,
    String resourceType,
    UUID resourceId,
    String description,
    String metadata,
    String correlationId,
    Instant createdAt
) {
    public static ActivityFeedResponse from(ActivityFeedItem item) {
        return new ActivityFeedResponse(
            item.getId(),
            item.getOrganizationId(),
            item.getProjectId(),
            item.getActorId(),
            item.getCategory(),
            item.getEventType(),
            item.getResourceType(),
            item.getResourceId(),
            item.getDescription(),
            item.getMetadata(),
            item.getCorrelationId(),
            item.getCreatedAt()
        );
    }
}

