package com.digitaloffice.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * Standardized cross-service platform event envelope.
 */
public record PlatformEventEnvelope(
    UUID eventId,
    String eventType,
    EventCategory category,
    String sourceService,
    UUID organizationId,
    UUID actorId,
    Instant occurredAt,
    String correlationId,
    int schemaVersion,
    String payload
) implements DomainEvent {

    public PlatformEventEnvelope {
        if (eventId == null) eventId = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
        if (schemaVersion <= 0) schemaVersion = 1;
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
    }

    @Override
    public UUID subjectId() {
        return eventId;
    }
}

