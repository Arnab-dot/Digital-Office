package com.digitaloffice.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID recipientId;

    private UUID organizationId;

    private UUID eventId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private UUID referenceId;

    @Column(nullable = false)
    private String referenceType;

    @Column(columnDefinition = "TEXT")
    private String extraData;

    @Column(nullable = false)
    private boolean readFlag;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;

    protected Notification() {}

    public Notification(UUID recipientId, String eventType, UUID referenceId, String referenceType, String extraData) {
        this(recipientId, null, null, "GENERAL", "NORMAL",
                formatDefaultTitle(eventType), formatDefaultMessage(eventType, referenceType),
                eventType, referenceId, referenceType, extraData, null);
    }

    public Notification(UUID recipientId, UUID organizationId, UUID eventId,
                        String category, String priority, String title, String message,
                        String eventType, UUID referenceId, String referenceType,
                        String extraData, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.recipientId = recipientId;
        this.organizationId = organizationId;
        this.eventId = eventId;
        this.category = (category != null && !category.isBlank()) ? category : "GENERAL";
        this.priority = (priority != null && !priority.isBlank()) ? priority : "NORMAL";
        this.title = (title != null && !title.isBlank()) ? title : formatDefaultTitle(eventType);
        this.message = (message != null && !message.isBlank()) ? message : formatDefaultMessage(eventType, referenceType);
        this.eventType = eventType;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.extraData = extraData;
        this.readFlag = false;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    private static String formatDefaultTitle(String eventType) {
        if (eventType == null) return "New Notification";
        String words = eventType.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static String formatDefaultMessage(String eventType, String referenceType) {
        return "You have a new update regarding " + (referenceType != null ? referenceType.toLowerCase() : "your item") + ".";
    }

    public void markRead() {
        this.readFlag = true;
    }

    public UUID getId() { return id; }
    public UUID getRecipientId() { return recipientId; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getEventId() { return eventId; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getEventType() { return eventType; }
    public UUID getReferenceId() { return referenceId; }
    public String getReferenceType() { return referenceType; }
    public String getExtraData() { return extraData; }
    public boolean isReadFlag() { return readFlag; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
