package com.digitaloffice.notification;

import com.digitaloffice.notification.dto.NotificationPreferenceDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService service;

    @MockBean
    private NotificationPreferenceService preferenceService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void testListNotifications_Authenticated() throws Exception {
        UUID actorId = UUID.randomUUID();
        Notification n = new Notification(actorId, "TASK_ASSIGNED", UUID.randomUUID(), "TASK", "{}");

        when(service.listNotifications(any(), eq("all"), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(n)));

        mockMvc.perform(get("/api/v1/notifications")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventType").value("TASK_ASSIGNED"));
    }

    @Test
    void testUnreadCount() throws Exception {
        when(service.getUnreadCount(any())).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    void testMarkRead() throws Exception {
        UUID notifId = UUID.randomUUID();
        Notification n = new Notification(UUID.randomUUID(), "TASK_ASSIGNED", UUID.randomUUID(), "TASK", "{}");
        n.markRead();

        when(service.markRead(eq(notifId), any())).thenReturn(n);

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notifId)
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readFlag").value(true));
    }

    @Test
    void testReadAll() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk());

        verify(service).markAllRead(any());
    }

    @Test
    void testGetPreferences() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationPreference pref = new NotificationPreference(userId);
        when(preferenceService.getPreferences(any())).thenReturn(pref);

        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled").value(true))
                .andExpect(jsonPath("$.emailEnabled").value(true));
    }

    @Test
    void testUpdatePreferences() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationPreference pref = new NotificationPreference(userId);
        pref.update(false, true, true, "{}", "23:00", "07:00", "UTC", "DAILY");

        when(preferenceService.updatePreferences(any(), eq(false), eq(true), eq(true),
                any(), any(), any(), any(), any())).thenReturn(pref);

        String json = """
                {
                    "inAppEnabled": false,
                    "emailEnabled": true,
                    "realTimeEnabled": true,
                    "categoryPreferences": "{}",
                    "quietHoursStart": "23:00",
                    "quietHoursEnd": "07:00",
                    "timeZone": "UTC",
                    "digestFrequency": "DAILY"
                }
                """;

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled").value(false));
    }
}

