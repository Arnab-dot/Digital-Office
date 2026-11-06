package com.digitaloffice.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    public NotificationPreference getPreferences(UUID userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> repository.save(new NotificationPreference(userId)));
    }

    @Transactional
    public NotificationPreference updatePreferences(UUID userId, boolean inAppEnabled,
                                                    boolean emailEnabled, boolean realTimeEnabled,
                                                    String categoryPreferences, String quietHoursStart,
                                                    String quietHoursEnd, String timeZone,
                                                    String digestFrequency) {
        NotificationPreference pref = getPreferences(userId);
        pref.update(inAppEnabled, emailEnabled, realTimeEnabled, categoryPreferences,
                quietHoursStart, quietHoursEnd, timeZone, digestFrequency);
        return repository.save(pref);
    }
}

