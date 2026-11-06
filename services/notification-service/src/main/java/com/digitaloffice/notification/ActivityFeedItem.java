package com.digitaloffice.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_feed")
public class ActivityFeedItem {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    private UUID projectId;

    @Column(nullable = false)
    private UUID actorId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private UUID resourceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    private String correlationId;

    @Column(nullable = false)
    private Instant createdAt;

    protected ActivityFeedItem() {}

    public ActivityFeedItem(UUID organizationId, UUID projectId, UUID actorId,
                            String category, String eventType, String resourceType,
                            UUID resourceId, String description, String metadata,
                            String correlationId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.actorId = actorId;
        this.category = (category != null && !category.isBlank()) ? category : "GENERAL";
        this.eventType = eventType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.description = description;
        this.metadata = metadata;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getProjectId() { return projectId; }
    public UUID getActorId() { return actorId; }
    public String getCategory() { return category; }
    public String getEventType() { return eventType; }
    public String getResourceType() { return resourceType; }
    public UUID getResourceId() { return resourceId; }
    public String getDescription() { return description; }
    public String getMetadata() { return metadata; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
}

