package com.digitaloffice.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ActivityFeedRepository extends JpaRepository<ActivityFeedItem, UUID> {
    Page<ActivityFeedItem> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<ActivityFeedItem> findByOrganizationIdAndCategory(UUID organizationId, String category, Pageable pageable);
    Page<ActivityFeedItem> findByProjectId(UUID projectId, Pageable pageable);
    Page<ActivityFeedItem> findByProjectIdAndCategory(UUID projectId, String category, Pageable pageable);
}

