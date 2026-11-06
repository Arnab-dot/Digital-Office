package com.digitaloffice.notification.dto;

import com.digitaloffice.notification.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID organizationId,
    UUID actorId,
    String action,
    String resourceType,
    UUID resourceId,
    String sourceService,
    String outcome,
    String correlationId,
    String metadata,
    Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getOrganizationId(),
            log.getActorId(),
            log.getAction(),
            log.getResourceType(),
            log.getResourceId(),
            log.getSourceService(),
            log.getOutcome(),
            log.getCorrelationId(),
            log.getMetadata(),
            log.getCreatedAt()
        );
    }
}

