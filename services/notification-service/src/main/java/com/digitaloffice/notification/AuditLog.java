package com.digitaloffice.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID actorId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private UUID resourceId;

    @Column(nullable = false)
    private String sourceService;

    @Column(nullable = false)
    private String outcome;

    private String correlationId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuditLog() {}

    public AuditLog(UUID organizationId, UUID actorId, String action,
                    String resourceType, UUID resourceId, String sourceService,
                    String outcome, String correlationId, String metadata) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.sourceService = sourceService;
        this.outcome = (outcome != null && !outcome.isBlank()) ? outcome : "SUCCESS";
        this.correlationId = correlationId;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public UUID getResourceId() { return resourceId; }
    public String getSourceService() { return sourceService; }
    public String getOutcome() { return outcome; }
    public String getCorrelationId() { return correlationId; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}

