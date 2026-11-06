package com.digitaloffice.notification.dto;

import com.digitaloffice.notification.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID recipientId,
    UUID organizationId,
    UUID eventId,
    String category,
    String priority,
    String title,
    String message,
    String eventType,
    UUID referenceId,
    String referenceType,
    String extraData,
    boolean readFlag,
    Instant createdAt,
    Instant expiresAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getId(),
            n.getRecipientId(),
            n.getOrganizationId(),
            n.getEventId(),
            n.getCategory(),
            n.getPriority(),
            n.getTitle(),
            n.getMessage(),
            n.getEventType(),
            n.getReferenceId(),
            n.getReferenceType(),
            n.getExtraData(),
            n.isReadFlag(),
            n.getCreatedAt(),
            n.getExpiresAt()
        );
    }
}

