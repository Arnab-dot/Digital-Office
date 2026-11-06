package com.digitaloffice.finance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionFeatureRepository featureRepository;

    public SubscriptionPlanService(SubscriptionPlanRepository planRepository,
                                   SubscriptionFeatureRepository featureRepository) {
        this.planRepository = planRepository;
        this.featureRepository = featureRepository;
    }

    public List<SubscriptionPlan> listActivePlans() {
        return planRepository.findByIsActiveTrueOrderByPriceAsc();
    }

    public SubscriptionPlan getPlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new NoSuchElementException("Subscription plan not found"));
    }

    public SubscriptionPlan getPlanByName(String name) {
        return planRepository.findByName(name)
                .orElseThrow(() -> new NoSuchElementException("Subscription plan not found: " + name));
    }

    @Transactional
    public SubscriptionPlan createPlan(String name, String displayName, String description,
                                       PlanTier tier, BillingInterval billingInterval,
                                       BigDecimal price, String currency,
                                       int maxMembers, int maxProjects, long storageLimitMb,
                                       int maxMeetingParticipants, int maxMeetingDurationMin,
                                       int aiRequestsLimit, int documentLimit, int trialDays) {
        if (planRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Plan name already exists: " + name);
        }
        SubscriptionPlan plan = new SubscriptionPlan(name, displayName, description, tier,
                billingInterval, price, currency, maxMembers, maxProjects, storageLimitMb,
                maxMeetingParticipants, maxMeetingDurationMin, aiRequestsLimit, documentLimit, trialDays);
        return planRepository.save(plan);
    }

    @Transactional
    public SubscriptionPlan updatePlan(UUID planId, String displayName, String description,
                                       BigDecimal price, int maxMembers, int maxProjects,
                                       long storageLimitMb, int maxMeetingParticipants,
                                       int maxMeetingDurationMin, int aiRequestsLimit,
                                       int documentLimit) {
        SubscriptionPlan plan = getPlan(planId);
        plan.update(displayName, description, price, maxMembers, maxProjects, storageLimitMb,
                maxMeetingParticipants, maxMeetingDurationMin, aiRequestsLimit, documentLimit);
        return planRepository.save(plan);
    }

    @Transactional
    public void deactivatePlan(UUID planId) {
        SubscriptionPlan plan = getPlan(planId);
        plan.deactivate();
        planRepository.save(plan);
    }

    public List<SubscriptionFeature> getFeatures(UUID planId) {
        return featureRepository.findByPlanId(planId);
    }

    @Transactional
    public SubscriptionFeature addFeature(UUID planId, String feature, boolean enabled) {
        getPlan(planId); // verify plan exists
        return featureRepository.save(new SubscriptionFeature(planId, feature, enabled));
    }
}
