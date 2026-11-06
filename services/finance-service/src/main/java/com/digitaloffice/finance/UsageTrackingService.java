package com.digitaloffice.finance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class UsageTrackingService {

    private final SubscriptionUsageRepository usageRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    public UsageTrackingService(SubscriptionUsageRepository usageRepository,
                                OrganizationSubscriptionRepository subscriptionRepository,
                                SubscriptionPlanRepository planRepository) {
        this.usageRepository = usageRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    public List<SubscriptionUsage> getUsage(UUID organizationId) {
        return usageRepository.findByOrganizationId(organizationId);
    }

    /**
     * Record a usage increment. Creates the usage record if it doesn't exist for the current period.
     */
    @Transactional
    public SubscriptionUsage recordUsage(UUID organizationId, UsageMetric metric, long delta) {
        SubscriptionUsage usage = usageRepository.findByOrganizationIdAndMetric(organizationId, metric)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    return usageRepository.save(new SubscriptionUsage(
                            organizationId, metric, 0,
                            now.truncatedTo(ChronoUnit.DAYS),
                            now.plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)));
                });
        usage.incrementBy(delta);
        return usageRepository.save(usage);
    }

    /**
     * Set the absolute current value for a metric (useful for snapshot-based tracking like member counts).
     */
    @Transactional
    public SubscriptionUsage setUsage(UUID organizationId, UsageMetric metric, long value) {
        SubscriptionUsage usage = usageRepository.findByOrganizationIdAndMetric(organizationId, metric)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    return usageRepository.save(new SubscriptionUsage(
                            organizationId, metric, 0,
                            now.truncatedTo(ChronoUnit.DAYS),
                            now.plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)));
                });
        usage.setValue(value);
        return usageRepository.save(usage);
    }

    /**
     * Check if adding the specified delta would exceed the plan limit for a given metric.
     * Returns true if within limit, false if exceeded.
     */
    public boolean checkEntitlement(UUID organizationId, UsageMetric metric, long delta) {
        OrganizationSubscription sub = subscriptionRepository.findByOrganizationId(organizationId)
                .orElse(null);
        if (sub == null) {
            return true; // no subscription = no enforcement (free tier default)
        }

        SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        if (plan == null) {
            return true;
        }

        long limit = getLimitForMetric(plan, metric);
        if (limit <= 0) {
            return true; // 0 or negative means unlimited
        }

        long current = usageRepository.findByOrganizationIdAndMetric(organizationId, metric)
                .map(SubscriptionUsage::getCurrentValue)
                .orElse(0L);

        return (current + delta) <= limit;
    }

    /**
     * Get the remaining quota for a metric.
     */
    public long getRemainingQuota(UUID organizationId, UsageMetric metric) {
        OrganizationSubscription sub = subscriptionRepository.findByOrganizationId(organizationId)
                .orElse(null);
        if (sub == null) return Long.MAX_VALUE;

        SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        if (plan == null) return Long.MAX_VALUE;

        long limit = getLimitForMetric(plan, metric);
        if (limit <= 0) return Long.MAX_VALUE;

        long current = usageRepository.findByOrganizationIdAndMetric(organizationId, metric)
                .map(SubscriptionUsage::getCurrentValue)
                .orElse(0L);

        return Math.max(0, limit - current);
    }

    private long getLimitForMetric(SubscriptionPlan plan, UsageMetric metric) {
        return switch (metric) {
            case MEMBERS -> plan.getMaxMembers();
            case STORAGE_MB -> plan.getStorageLimitMb();
            case PROJECTS -> plan.getMaxProjects();
            case MEETINGS -> 0; // unlimited by default
            case MEETING_MINUTES -> (long) plan.getMaxMeetingDurationMin() * plan.getMaxMeetingParticipants();
            case AI_REQUESTS -> plan.getAiRequestsLimit();
            case DOCUMENTS -> plan.getDocumentLimit();
            case API_CALLS -> 0; // unlimited by default
        };
    }
}
