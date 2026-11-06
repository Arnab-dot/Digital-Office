package com.digitaloffice.notification.dto;

import com.digitaloffice.notification.NotificationPreference;
import java.time.Instant;
import java.util.UUID;

public record NotificationPreferenceDto(
    UUID id,
    UUID userId,
    boolean inAppEnabled,
    boolean emailEnabled,
    boolean realTimeEnabled,
    String categoryPreferences,
    String quietHoursStart,
    String quietHoursEnd,
    String timeZone,
    String digestFrequency,
    Instant updatedAt
) {
    public static NotificationPreferenceDto from(NotificationPreference p) {
        return new NotificationPreferenceDto(
            p.getId(),
            p.getUserId(),
            p.isInAppEnabled(),
            p.isEmailEnabled(),
            p.isRealTimeEnabled(),
            p.getCategoryPreferences(),
            p.getQuietHoursStart(),
            p.getQuietHoursEnd(),
            p.getTimeZone(),
            p.getDigestFrequency(),
            p.getUpdatedAt()
        );
    }
}

