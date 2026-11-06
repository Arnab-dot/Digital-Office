package com.digitaloffice.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, UUID> {
    Page<SubscriptionHistory> findByOrganizationId(UUID organizationId, Pageable pageable);
    List<SubscriptionHistory> findBySubscriptionId(UUID subscriptionId);
}
