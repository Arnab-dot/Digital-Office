package com.digitaloffice.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private boolean inAppEnabled;

    @Column(nullable = false)
    private boolean emailEnabled;

    @Column(nullable = false)
    private boolean realTimeEnabled;

    @Column(columnDefinition = "TEXT")
    private String categoryPreferences;

    private String quietHoursStart;

    private String quietHoursEnd;

    @Column(nullable = false)
    private String timeZone;

    @Column(nullable = false)
    private String digestFrequency;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected NotificationPreference() {}

    public NotificationPreference(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.inAppEnabled = true;
        this.emailEnabled = true;
        this.realTimeEnabled = true;
        this.categoryPreferences = "{}";
        this.timeZone = "UTC";
        this.digestFrequency = "INSTANT";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(boolean inAppEnabled, boolean emailEnabled, boolean realTimeEnabled,
                       String categoryPreferences, String quietHoursStart, String quietHoursEnd,
                       String timeZone, String digestFrequency) {
        this.inAppEnabled = inAppEnabled;
        this.emailEnabled = emailEnabled;
        this.realTimeEnabled = realTimeEnabled;
        if (categoryPreferences != null) this.categoryPreferences = categoryPreferences;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
        if (timeZone != null && !timeZone.isBlank()) this.timeZone = timeZone;
        if (digestFrequency != null && !digestFrequency.isBlank()) this.digestFrequency = digestFrequency;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public boolean isInAppEnabled() { return inAppEnabled; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public boolean isRealTimeEnabled() { return realTimeEnabled; }
    public String getCategoryPreferences() { return categoryPreferences; }
    public String getQuietHoursStart() { return quietHoursStart; }
    public String getQuietHoursEnd() { return quietHoursEnd; }
    public String getTimeZone() { return timeZone; }
    public String getDigestFrequency() { return digestFrequency; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

