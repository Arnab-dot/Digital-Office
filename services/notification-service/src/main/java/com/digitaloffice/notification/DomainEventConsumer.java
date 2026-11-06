package com.digitaloffice.notification;

import com.digitaloffice.contracts.PlatformEventEnvelope;
import com.digitaloffice.contracts.UserNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Component
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);

    private static final Set<String> AUDITABLE_EVENTS = Set.of(
            "ROLE_CHANGED",
            "MEMBER_REMOVED",
            "ORGANIZATION_UPDATED",
            "PROJECT_CLOSED",
            "DOCUMENT_REVIEW_COMPLETED",
            "DOCUMENT_APPROVED",
            "DOCUMENT_REJECTED",
            "SUBSCRIPTION_CREATED",
            "PLAN_CHANGED",
            "PAYMENT_CONFIRMED",
            "PAYMENT_FAILED",
            "REFUND_PROCESSED"
    );

    private final NotificationService notifications;
    private final ActivityFeedService activityFeed;
    private final AuditLogService auditLogs;
    private final ProcessedEventRepository processedEvents;

    public DomainEventConsumer(NotificationService notifications,
                               ActivityFeedService activityFeed,
                               AuditLogService auditLogs,
                               ProcessedEventRepository processedEvents) {
        this.notifications = notifications;
        this.activityFeed = activityFeed;
        this.auditLogs = auditLogs;
        this.processedEvents = processedEvents;
    }

    @EventListener
    @Transactional
    public void onUserNotificationEvent(UserNotificationEvent event) {
        UUID eventId = UUID.nameUUIDFromBytes(
                (event.eventType() + ":" + event.recipientId() + ":" + event.occurredAt()).getBytes());

        if (processedEvents.existsById(eventId)) {
            log.debug("Skipping already processed user notification event {}", eventId);
            return;
        }

        notifications.createNotification(
                event.recipientId(),
                event.eventType(),
                event.subjectId(),
                event.referenceType(),
                event.extraData()
        );

        processedEvents.save(new ProcessedEvent(eventId, "in-process", event.eventType()));
    }

    @EventListener
    @Transactional
    public void onPlatformEnvelope(PlatformEventEnvelope envelope) {
        if (processedEvents.existsById(envelope.eventId())) {
            log.debug("Skipping duplicate envelope event {}", envelope.eventId());
            return;
        }

        // 1. Record in Activity Feed if organization is present
        if (envelope.organizationId() != null) {
            String category = envelope.category() != null ? envelope.category().name() : "GENERAL";
            activityFeed.recordActivity(
                    envelope.organizationId(),
                    null, // project ID extracted from payload if needed
                    envelope.actorId(),
                    category,
                    envelope.eventType(),
                    "PLATFORM_EVENT",
                    envelope.eventId(),
                    formatActivityDescription(envelope),
                    envelope.payload(),
                    envelope.correlationId()
            );
        }

        // 2. Record in Audit Log if auditable
        if (envelope.organizationId() != null && AUDITABLE_EVENTS.contains(envelope.eventType())) {
            auditLogs.recordAudit(
                    envelope.organizationId(),
                    envelope.actorId(),
                    envelope.eventType(),
                    "PLATFORM_EVENT",
                    envelope.eventId(),
                    envelope.sourceService(),
                    "SUCCESS",
                    envelope.correlationId(),
                    envelope.payload()
            );
        }

        processedEvents.save(new ProcessedEvent(envelope.eventId(), envelope.sourceService(), envelope.eventType()));
    }

    private String formatActivityDescription(PlatformEventEnvelope envelope) {
        String eventName = envelope.eventType().replace('_', ' ').toLowerCase();
        return "Event recorded: " + eventName + " by user " + envelope.actorId();
    }
}
