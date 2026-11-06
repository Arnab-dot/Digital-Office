package com.digitaloffice.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityFeedServiceTest {

    @Mock
    private ActivityFeedRepository repository;

    private ActivityFeedService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ActivityFeedService(repository);
    }

    @Test
    void testRecordActivity() {
        when(repository.save(any(ActivityFeedItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityFeedItem item = service.recordActivity(
                orgId, projectId, actorId, "PROJECT", "PROJECT_CREATED",
                "PROJECT", projectId, "Created new project", "{}", "corr-1"
        );

        assertNotNull(item);
        assertEquals(orgId, item.getOrganizationId());
        assertEquals(projectId, item.getProjectId());
        assertEquals("PROJECT_CREATED", item.getEventType());
    }

    @Test
    void testGetOrganizationActivity() {
        ActivityFeedItem item = new ActivityFeedItem(
                orgId, null, actorId, "ORGANIZATION", "MEMBER_JOINED",
                "USER", actorId, "New member joined", null, null
        );
        when(repository.findByOrganizationId(eq(orgId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        Page<ActivityFeedItem> page = service.getOrganizationActivity(orgId, null, 0, 20);

        assertEquals(1, page.getTotalElements());
        assertEquals("MEMBER_JOINED", page.getContent().get(0).getEventType());
    }
}

