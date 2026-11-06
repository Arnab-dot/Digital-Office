package com.digitaloffice.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository repository;

    private NotificationPreferenceService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceService(repository);
    }

    @Test
    void testGetPreferences_CreatesDefaultIfMissing() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any(NotificationPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationPreference pref = service.getPreferences(userId);

        assertNotNull(pref);
        assertEquals(userId, pref.getUserId());
        assertTrue(pref.isInAppEnabled());
        assertTrue(pref.isEmailEnabled());
        assertTrue(pref.isRealTimeEnabled());
    }

    @Test
    void testUpdatePreferences() {
        NotificationPreference existing = new NotificationPreference(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(repository.save(any(NotificationPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationPreference updated = service.updatePreferences(
                userId, false, false, true, "{\"WORK\":true}",
                "22:00", "07:00", "America/New_York", "DAILY_DIGEST"
        );

        assertFalse(updated.isInAppEnabled());
        assertFalse(updated.isEmailEnabled());
        assertTrue(updated.isRealTimeEnabled());
        assertEquals("22:00", updated.getQuietHoursStart());
        assertEquals("07:00", updated.getQuietHoursEnd());
        assertEquals("America/New_York", updated.getTimeZone());
        assertEquals("DAILY_DIGEST", updated.getDigestFrequency());
    }
}

