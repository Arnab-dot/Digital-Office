package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.BillingInterval;
import com.digitaloffice.finance.PlanTier;
import com.digitaloffice.finance.SubscriptionFeature;
import com.digitaloffice.finance.SubscriptionPlan;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionPlanResponse(
    UUID id,
    String name,
    String displayName,
    String description,
    PlanTier tier,
    BillingInterval billingInterval,
    BigDecimal price,
    String currency,
    int maxMembers,
    int maxProjects,
    long storageLimitMb,
    int maxMeetingParticipants,
    int maxMeetingDurationMin,
    int aiRequestsLimit,
    int documentLimit,
    boolean isActive,
    int trialDays,
    List<String> features,
    Instant createdAt,
    Instant updatedAt
) {
    public static SubscriptionPlanResponse from(SubscriptionPlan plan, List<SubscriptionFeature> features) {
        List<String> featureNames = features != null
            ? features.stream().filter(SubscriptionFeature::isEnabled).map(SubscriptionFeature::getFeature).toList()
            : List.of();
        return new SubscriptionPlanResponse(
            plan.getId(),
            plan.getName(),
            plan.getDisplayName(),
            plan.getDescription(),
            plan.getTier(),
            plan.getBillingInterval(),
            plan.getPrice(),
            plan.getCurrency(),
            plan.getMaxMembers(),
            plan.getMaxProjects(),
            plan.getStorageLimitMb(),
            plan.getMaxMeetingParticipants(),
            plan.getMaxMeetingDurationMin(),
            plan.getAiRequestsLimit(),
            plan.getDocumentLimit(),
            plan.isActive(),
            plan.getTrialDays(),
            featureNames,
            plan.getCreatedAt(),
            plan.getUpdatedAt()
        );
    }
}

