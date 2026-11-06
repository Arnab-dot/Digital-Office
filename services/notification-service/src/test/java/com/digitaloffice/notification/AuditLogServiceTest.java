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
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository repository;

    private AuditLogService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AuditLogService(repository);
    }

    @Test
    void testRecordAudit_Immutable() {
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog log = service.recordAudit(
                orgId, actorId, "MEMBER_REMOVED", "USER",
                UUID.randomUUID(), "organization-service", "SUCCESS", "corr-99", "{\"reason\":\"resigned\"}"
        );

        assertNotNull(log);
        assertEquals(orgId, log.getOrganizationId());
        assertEquals(actorId, log.getActorId());
        assertEquals("MEMBER_REMOVED", log.getAction());
        assertEquals("SUCCESS", log.getOutcome());
    }

    @Test
    void testGetOrganizationAuditLogs() {
        AuditLog log = new AuditLog(
                orgId, actorId, "ROLE_CHANGED", "MEMBERSHIP",
                UUID.randomUUID(), "organization-service", "SUCCESS", null, null
        );
        when(repository.findByOrganizationId(eq(orgId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLog> page = service.getOrganizationAuditLogs(orgId, null, null, 0, 20);

        assertEquals(1, page.getTotalElements());
        assertEquals("ROLE_CHANGED", page.getContent().get(0).getAction());
    }
}

