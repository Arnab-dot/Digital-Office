package com.digitaloffice.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationPreferenceService preferenceService;
    private final WebSocketNotificationService webSocketService;

    public NotificationService(NotificationRepository repository,
                               NotificationPreferenceService preferenceService,
                               @Autowired(required = false) WebSocketNotificationService webSocketService) {
        this.repository = repository;
        this.preferenceService = preferenceService;
        this.webSocketService = webSocketService;
    }

    public Page<Notification> listNotifications(UUID actor, String status, int page, int size) {
        int bounded = Math.min(Math.max(size, 1), 50);
        Pageable p = PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt"));
        return switch (status == null ? "all" : status.toLowerCase()) {
            case "read" -> repository.findByRecipientIdAndReadFlag(actor, true, p);
            case "unread" -> repository.findByRecipientIdAndReadFlag(actor, false, p);
            case "all" -> repository.findByRecipientId(actor, p);
            default -> throw new IllegalArgumentException("readStatus must be read, unread, or all");
        };
    }

    public long getUnreadCount(UUID actor) {
        return repository.countByRecipientIdAndReadFlag(actor, false);
    }

    @Transactional
    public Notification markRead(UUID id, UUID actor) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notification not found"));
        if (!actor.equals(n.getRecipientId())) {
            throw new AccessDeniedException("Notification belongs to another user");
        }
        n.markRead();
        Notification saved = repository.save(n);
        if (webSocketService != null) {
            webSocketService.sendUnreadCount(actor, getUnreadCount(actor));
        }
        return saved;
    }

    @Transactional
    public void markAllRead(UUID actor) {
        Page<Notification> unreadPage = repository.findByRecipientIdAndReadFlag(
                actor, false, PageRequest.of(0, 100));
        for (Notification n : unreadPage.getContent()) {
            n.markRead();
        }
        repository.saveAll(unreadPage.getContent());
        if (webSocketService != null) {
            webSocketService.sendUnreadCount(actor, 0L);
        }
    }

    @Transactional
    public Notification createNotification(UUID recipientId, String eventType, UUID referenceId,
                                           String referenceType, String extraData) {
        return createNotification(recipientId, null, null, "GENERAL", "NORMAL",
                null, null, eventType, referenceId, referenceType, extraData, null);
    }

    @Transactional
    public Notification createNotification(UUID recipientId, UUID organizationId, UUID eventId,
                                           String category, String priority, String title,
                                           String message, String eventType, UUID referenceId,
                                           String referenceType, String extraData, Instant expiresAt) {
        NotificationPreference pref = preferenceService.getPreferences(recipientId);
        if (!pref.isInAppEnabled()) {
            return null;
        }

        Notification n = new Notification(
                recipientId, organizationId, eventId, category, priority,
                title, message, eventType, referenceId, referenceType, extraData, expiresAt
        );
        n = repository.save(n);

        if (webSocketService != null && pref.isRealTimeEnabled()) {
            webSocketService.sendToUser(recipientId, n);
            webSocketService.sendUnreadCount(recipientId, getUnreadCount(recipientId));
        }

        return n;
    }
}
