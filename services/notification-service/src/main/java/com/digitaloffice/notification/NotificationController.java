package com.digitaloffice.notification;

import com.digitaloffice.notification.dto.NotificationPreferenceDto;
import com.digitaloffice.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationPreferenceService preferenceService;

    public NotificationController(NotificationService service,
                                  NotificationPreferenceService preferenceService) {
        this.service = service;
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public Page<NotificationResponse> list(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(defaultValue = "all") String readStatus,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.listNotifications(actor(jwt), readStatus, page, size)
                .map(NotificationResponse::from);
    }

    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("unreadCount", service.getUnreadCount(actor(jwt)));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse read(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return NotificationResponse.from(service.markRead(id, actor(jwt)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll(@AuthenticationPrincipal Jwt jwt) {
        service.markAllRead(actor(jwt));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preferences")
    public NotificationPreferenceDto getPreferences(@AuthenticationPrincipal Jwt jwt) {
        return NotificationPreferenceDto.from(preferenceService.getPreferences(actor(jwt)));
    }

    @PutMapping("/preferences")
    public NotificationPreferenceDto updatePreferences(@AuthenticationPrincipal Jwt jwt,
                                                       @RequestBody NotificationPreferenceDto body) {
        NotificationPreference updated = preferenceService.updatePreferences(
                actor(jwt),
                body.inAppEnabled(),
                body.emailEnabled(),
                body.realTimeEnabled(),
                body.categoryPreferences(),
                body.quietHoursStart(),
                body.quietHoursEnd(),
                body.timeZone(),
                body.digestFrequency()
        );
        return NotificationPreferenceDto.from(updated);
    }

    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
    }
}
