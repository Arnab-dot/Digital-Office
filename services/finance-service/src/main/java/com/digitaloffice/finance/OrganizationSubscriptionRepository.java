package com.digitaloffice.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, UUID> {
    Optional<OrganizationSubscription> findByOrganizationId(UUID organizationId);
    List<OrganizationSubscription> findByStatus(SubscriptionStatus status);
}
