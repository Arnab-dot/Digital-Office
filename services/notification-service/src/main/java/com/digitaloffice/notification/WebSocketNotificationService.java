package com.digitaloffice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToUser(UUID userId, Notification notification) {
        try {
            String destination = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("Sent real-time notification {} to {}", notification.getId(), destination);
        } catch (Exception ex) {
            log.warn("Failed to deliver real-time WebSocket notification to user {}: {}", userId, ex.getMessage());
        }
    }

    public void sendUnreadCount(UUID userId, long unreadCount) {
        try {
            String destination = "/topic/notifications/" + userId + "/count";
            messagingTemplate.convertAndSend(destination, Map.of("unreadCount", unreadCount));
            log.debug("Sent unread count {} to {}", unreadCount, destination);
        } catch (Exception ex) {
            log.warn("Failed to deliver unread count to user {}: {}", userId, ex.getMessage());
        }
    }
}

