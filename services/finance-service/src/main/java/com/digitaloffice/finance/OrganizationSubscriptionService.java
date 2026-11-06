package com.digitaloffice.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class OrganizationSubscriptionService {

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final SubscriptionUsageRepository usageRepository;

    public OrganizationSubscriptionService(OrganizationSubscriptionRepository subscriptionRepository,
                                           SubscriptionPlanRepository planRepository,
                                           SubscriptionHistoryRepository historyRepository,
                                           SubscriptionUsageRepository usageRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.historyRepository = historyRepository;
        this.usageRepository = usageRepository;
    }

    public OrganizationSubscription getSubscription(UUID organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new NoSuchElementException("No subscription found for organization"));
    }

    @Transactional
    public OrganizationSubscription createSubscription(UUID organizationId, UUID planId,
                                                        BillingInterval billingInterval, UUID actorId) {
        if (subscriptionRepository.findByOrganizationId(organizationId).isPresent()) {
            throw new IllegalStateException("Organization already has a subscription");
        }

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NoSuchElementException("Plan not found"));

        Instant now = Instant.now();
        SubscriptionStatus initialStatus;
        Instant periodEnd;
        Instant trialEnd = null;

        if (plan.getTrialDays() > 0) {
            initialStatus = SubscriptionStatus.TRIAL;
            trialEnd = now.plus(plan.getTrialDays(), ChronoUnit.DAYS);
            periodEnd = trialEnd;
        } else {
            initialStatus = SubscriptionStatus.ACTIVE;
            periodEnd = billingInterval == BillingInterval.ANNUAL
                    ? now.plus(365, ChronoUnit.DAYS)
                    : now.plus(30, ChronoUnit.DAYS);
        }

        OrganizationSubscription sub = new OrganizationSubscription(
                organizationId, planId, initialStatus, billingInterval, now, periodEnd, trialEnd);
        sub = subscriptionRepository.save(sub);

        SubscriptionAction action = plan.getTrialDays() > 0
                ? SubscriptionAction.TRIAL_STARTED : SubscriptionAction.CREATED;
        historyRepository.save(new SubscriptionHistory(
                sub.getId(), organizationId, action, null, planId, actorId, null));

        return sub;
    }

    @Transactional
    public OrganizationSubscription changePlan(UUID organizationId, UUID newPlanId, UUID actorId) {
        OrganizationSubscription sub = getSubscription(organizationId);
        SubscriptionPlan newPlan = planRepository.findById(newPlanId)
                .orElseThrow(() -> new NoSuchElementException("New plan not found"));

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot change plan for a cancelled subscription");
        }

        UUID oldPlanId = sub.getPlanId();
        SubscriptionPlan oldPlan = planRepository.findById(oldPlanId).orElse(null);

        SubscriptionAction action;
        if (oldPlan != null && newPlan.getPrice().compareTo(oldPlan.getPrice()) > 0) {
            action = SubscriptionAction.UPGRADED;
        } else if (oldPlan != null && newPlan.getPrice().compareTo(oldPlan.getPrice()) < 0) {
            action = SubscriptionAction.DOWNGRADED;
        } else {
            action = SubscriptionAction.UPGRADED; // same price = lateral move, treat as upgrade
        }

        validateDowngradeUsage(organizationId, newPlan);

        sub.changePlan(newPlanId);
        if (sub.getStatus() == SubscriptionStatus.TRIAL) {
            sub.activate();
        }
        sub = subscriptionRepository.save(sub);

        historyRepository.save(new SubscriptionHistory(
                sub.getId(), organizationId, action, oldPlanId, newPlanId, actorId, null));

        return sub;
    }

    @Transactional
    public OrganizationSubscription cancelSubscription(UUID organizationId, UUID actorId) {
        OrganizationSubscription sub = getSubscription(organizationId);
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Subscription is already cancelled");
        }
        UUID planId = sub.getPlanId();
        sub.cancel();
        sub = subscriptionRepository.save(sub);

        historyRepository.save(new SubscriptionHistory(
                sub.getId(), organizationId, SubscriptionAction.CANCELLED,
                planId, null, actorId, null));

        return sub;
    }

    @Transactional
    public OrganizationSubscription renewSubscription(UUID organizationId, UUID actorId) {
        OrganizationSubscription sub = getSubscription(organizationId);
        if (sub.getStatus() != SubscriptionStatus.ACTIVE && sub.getStatus() != SubscriptionStatus.PAST_DUE) {
            throw new IllegalStateException("Only active or past-due subscriptions can be renewed");
        }

        Instant newStart = sub.getCurrentPeriodEnd();
        Instant newEnd = sub.getBillingInterval() == BillingInterval.ANNUAL
                ? newStart.plus(365, ChronoUnit.DAYS)
                : newStart.plus(30, ChronoUnit.DAYS);

        sub.renew(newStart, newEnd);
        sub = subscriptionRepository.save(sub);

        historyRepository.save(new SubscriptionHistory(
                sub.getId(), organizationId, SubscriptionAction.RENEWED,
                sub.getPlanId(), sub.getPlanId(), actorId, null));

        return sub;
    }

    @Transactional
    public OrganizationSubscription reactivateSubscription(UUID organizationId, UUID actorId) {
        OrganizationSubscription sub = getSubscription(organizationId);
        if (sub.getStatus() != SubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended subscriptions can be reactivated");
        }
        sub.reactivate();
        sub = subscriptionRepository.save(sub);

        historyRepository.save(new SubscriptionHistory(
                sub.getId(), organizationId, SubscriptionAction.REACTIVATED,
                sub.getPlanId(), sub.getPlanId(), actorId, null));

        return sub;
    }

    public Page<SubscriptionHistory> getHistory(UUID organizationId, int page, int size) {
        int bounded = Math.min(Math.max(size, 1), 50);
        return historyRepository.findByOrganizationId(organizationId,
                PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private void validateDowngradeUsage(UUID organizationId, SubscriptionPlan newPlan) {
        if (newPlan.getMaxMembers() > 0) {
            long currentMembers = usageRepository.findByOrganizationIdAndMetric(organizationId, UsageMetric.MEMBERS)
                    .map(SubscriptionUsage::getCurrentValue).orElse(0L);
            if (currentMembers > newPlan.getMaxMembers()) {
                throw new IllegalStateException("Cannot downgrade plan: active members (" + currentMembers
                        + ") exceed new plan limit (" + newPlan.getMaxMembers() + ")");
            }
        }
        if (newPlan.getMaxProjects() > 0) {
            long currentProjects = usageRepository.findByOrganizationIdAndMetric(organizationId, UsageMetric.PROJECTS)
                    .map(SubscriptionUsage::getCurrentValue).orElse(0L);
            if (currentProjects > newPlan.getMaxProjects()) {
                throw new IllegalStateException("Cannot downgrade plan: active projects (" + currentProjects
                        + ") exceed new plan limit (" + newPlan.getMaxProjects() + ")");
            }
        }
        if (newPlan.getStorageLimitMb() > 0) {
            long currentStorage = usageRepository.findByOrganizationIdAndMetric(organizationId, UsageMetric.STORAGE_MB)
                    .map(SubscriptionUsage::getCurrentValue).orElse(0L);
            if (currentStorage > newPlan.getStorageLimitMb()) {
                throw new IllegalStateException("Cannot downgrade plan: storage consumption (" + currentStorage
                        + " MB) exceeds new plan limit (" + newPlan.getStorageLimitMb() + " MB)");
            }
        }
        if (newPlan.getDocumentLimit() > 0) {
            long currentDocs = usageRepository.findByOrganizationIdAndMetric(organizationId, UsageMetric.DOCUMENTS)
                    .map(SubscriptionUsage::getCurrentValue).orElse(0L);
            if (currentDocs > newPlan.getDocumentLimit()) {
                throw new IllegalStateException("Cannot downgrade plan: document count (" + currentDocs
                        + ") exceeds new plan limit (" + newPlan.getDocumentLimit() + ")");
            }
        }
    }
}
