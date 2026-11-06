package com.digitaloffice.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditLog recordAudit(UUID organizationId, UUID actorId, String action,
                                String resourceType, UUID resourceId, String sourceService,
                                String outcome, String correlationId, String metadata) {
        AuditLog log = new AuditLog(
                organizationId, actorId, action, resourceType, resourceId,
                sourceService, outcome, correlationId, metadata
        );
        return repository.save(log);
    }

    public Page<AuditLog> getOrganizationAuditLogs(UUID organizationId, String action,
                                                  UUID actorId, int page, int size) {
        int bounded = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (action != null && !action.isBlank()) {
            return repository.findByOrganizationIdAndAction(organizationId, action, pageable);
        }
        if (actorId != null) {
            return repository.findByOrganizationIdAndActorId(organizationId, actorId, pageable);
        }
        return repository.findByOrganizationId(organizationId, pageable);
    }
}

