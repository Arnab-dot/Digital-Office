package com.digitaloffice.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for all domain events published through the platform event bus.
 * Implementations are emitted by Work_Service, Document_Service, and Project_Service
 * and consumed by Notification_Service (and future Kafka consumers).
 */
public interface DomainEvent {

    /** Identifies the type of event, e.g. "TASK_ASSIGNED", "REVIEW_REQUESTED". */
    String eventType();

    /** The primary resource affected by this event (task ID, document version ID, etc.). */
    UUID subjectId();

    /** The authenticated user who triggered the event. */
    UUID actorId();

    /** Wall-clock time at which the event occurred. */
    Instant occurredAt();
}
