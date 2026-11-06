package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.BillingInterval;
import com.digitaloffice.finance.OrganizationSubscription;
import com.digitaloffice.finance.SubscriptionPlan;
import com.digitaloffice.finance.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID organizationId,
    UUID planId,
    String planName,
    String planDisplayName,
    SubscriptionStatus status,
    BillingInterval billingInterval,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    Instant trialEnd,
    Instant cancelledAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static SubscriptionResponse from(OrganizationSubscription sub, SubscriptionPlan plan) {
        return new SubscriptionResponse(
            sub.getId(),
            sub.getOrganizationId(),
            sub.getPlanId(),
            plan != null ? plan.getName() : null,
            plan != null ? plan.getDisplayName() : null,
            sub.getStatus(),
            sub.getBillingInterval(),
            sub.getCurrentPeriodStart(),
            sub.getCurrentPeriodEnd(),
            sub.getTrialEnd(),
            sub.getCancelledAt(),
            sub.getCreatedAt(),
            sub.getUpdatedAt()
        );
    }
}

