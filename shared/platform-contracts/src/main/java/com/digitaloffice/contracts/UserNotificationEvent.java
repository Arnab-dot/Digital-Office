package com.digitaloffice.contracts;

import java.time.Instant;
import java.util.UUID;

/** A domain event that directly addresses one user for notification delivery. */
public record UserNotificationEvent(String eventType, UUID subjectId, UUID actorId, Instant occurredAt,
                                    UUID recipientId, String referenceType, String extraData)
        implements DomainEvent {
    public UserNotificationEvent {
        if (eventType == null || eventType.isBlank() || subjectId == null || actorId == null
                || occurredAt == null || recipientId == null || referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("Notification event fields are required");
        }
    }
}
