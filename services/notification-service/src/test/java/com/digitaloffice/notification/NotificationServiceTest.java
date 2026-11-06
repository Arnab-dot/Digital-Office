package com.digitaloffice.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;
    @Mock
    private NotificationPreferenceService preferenceService;
    @Mock
    private WebSocketNotificationService webSocketService;

    private NotificationService service;

    private final UUID recipientId = UUID.randomUUID();
    private final UUID otherUser = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, preferenceService, webSocketService);
    }

    @Test
    void testCreateNotification_WhenInAppEnabled_SavesAndPushes() {
        NotificationPreference pref = new NotificationPreference(recipientId);
        when(preferenceService.getPreferences(recipientId)).thenReturn(pref);
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = service.createNotification(
                recipientId, "TASK_ASSIGNED", UUID.randomUUID(), "TASK", "{}"
        );

        assertNotNull(result);
        assertEquals(recipientId, result.getRecipientId());
        assertEquals("TASK_ASSIGNED", result.getEventType());
        verify(webSocketService).sendToUser(eq(recipientId), any(Notification.class));
        verify(webSocketService).sendUnreadCount(eq(recipientId), anyLong());
    }

    @Test
    void testCreateNotification_WhenInAppDisabled_ReturnsNull() {
        NotificationPreference pref = new NotificationPreference(recipientId);
        pref.update(false, true, true, "{}", null, null, "UTC", "INSTANT");
        when(preferenceService.getPreferences(recipientId)).thenReturn(pref);

        Notification result = service.createNotification(
                recipientId, "TASK_ASSIGNED", UUID.randomUUID(), "TASK", "{}"
        );

        assertNull(result);
        verify(repository, never()).save(any());
        verify(webSocketService, never()).sendToUser(any(), any());
    }

    @Test
    void testMarkRead_Authorized() {
        Notification n = new Notification(recipientId, "TASK_ASSIGNED", UUID.randomUUID(), "TASK", "{}");
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification read = service.markRead(n.getId(), recipientId);

        assertTrue(read.isReadFlag());
        verify(webSocketService).sendUnreadCount(eq(recipientId), anyLong());
    }

    @Test
    void testMarkRead_AccessDeniedWhenNotRecipient() {
        Notification n = new Notification(recipientId, "TASK_ASSIGNED", UUID.randomUUID(), "TASK", "{}");
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));

        assertThrows(AccessDeniedException.class, () -> service.markRead(n.getId(), otherUser));
    }

    @Test
    void testMarkAllRead() {
        Notification n1 = new Notification(recipientId, "EVENT1", UUID.randomUUID(), "TYPE", "{}");
        Notification n2 = new Notification(recipientId, "EVENT2", UUID.randomUUID(), "TYPE", "{}");
        when(repository.findByRecipientIdAndReadFlag(eq(recipientId), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n1, n2)));

        service.markAllRead(recipientId);

        assertTrue(n1.isReadFlag());
        assertTrue(n2.isReadFlag());
        verify(repository).saveAll(any());
        verify(webSocketService).sendUnreadCount(recipientId, 0L);
    }

    @Test
    void testGetUnreadCount() {
        when(repository.countByRecipientIdAndReadFlag(recipientId, false)).thenReturn(5L);
        assertEquals(5L, service.getUnreadCount(recipientId));
    }
}

