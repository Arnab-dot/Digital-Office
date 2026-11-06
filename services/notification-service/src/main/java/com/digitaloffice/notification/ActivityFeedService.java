package com.digitaloffice.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivityFeedService {

    private final ActivityFeedRepository repository;

    public ActivityFeedService(ActivityFeedRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ActivityFeedItem recordActivity(UUID organizationId, UUID projectId, UUID actorId,
                                           String category, String eventType, String resourceType,
                                           UUID resourceId, String description, String metadata,
                                           String correlationId) {
        ActivityFeedItem item = new ActivityFeedItem(
                organizationId, projectId, actorId, category, eventType,
                resourceType, resourceId, description, metadata, correlationId
        );
        return repository.save(item);
    }

    public Page<ActivityFeedItem> getOrganizationActivity(UUID organizationId, String category, int page, int size) {
        int bounded = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            return repository.findByOrganizationIdAndCategory(organizationId, category.toUpperCase(), pageable);
        }
        return repository.findByOrganizationId(organizationId, pageable);
    }

    public Page<ActivityFeedItem> getProjectActivity(UUID projectId, String category, int page, int size) {
        int bounded = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            return repository.findByProjectIdAndCategory(projectId, category.toUpperCase(), pageable);
        }
        return repository.findByProjectId(projectId, pageable);
    }
}

