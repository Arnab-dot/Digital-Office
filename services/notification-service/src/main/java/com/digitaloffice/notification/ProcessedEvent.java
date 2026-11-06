package com.digitaloffice.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private UUID eventId;

    private String sourceService;

    private String eventType;

    @Column(nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(UUID eventId, String sourceService, String eventType) {
        this.eventId = eventId;
        this.sourceService = sourceService;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public String getSourceService() { return sourceService; }
    public String getEventType() { return eventType; }
    public Instant getProcessedAt() { return processedAt; }
}

