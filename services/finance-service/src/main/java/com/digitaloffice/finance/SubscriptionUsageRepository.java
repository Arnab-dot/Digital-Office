package com.digitaloffice.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionUsageRepository extends JpaRepository<SubscriptionUsage, UUID> {
    List<SubscriptionUsage> findByOrganizationId(UUID organizationId);
    Optional<SubscriptionUsage> findByOrganizationIdAndMetric(UUID organizationId, UsageMetric metric);
}
