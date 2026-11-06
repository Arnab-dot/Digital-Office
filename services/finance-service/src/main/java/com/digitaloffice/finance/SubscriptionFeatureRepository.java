package com.digitaloffice.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SubscriptionFeatureRepository extends JpaRepository<SubscriptionFeature, UUID> {
    List<SubscriptionFeature> findByPlanId(UUID planId);
    void deleteByPlanId(UUID planId);
}
