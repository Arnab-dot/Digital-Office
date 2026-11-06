package com.digitaloffice.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageTrackingServiceTest {

    @Mock
    private SubscriptionUsageRepository usageRepository;
    @Mock
    private OrganizationSubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPlanRepository planRepository;

    private UsageTrackingService service;

    private final UUID orgId = UUID.randomUUID();
    private UUID planId;
    private SubscriptionPlan plan;
    private OrganizationSubscription sub;

    @BeforeEach
    void setUp() {
        service = new UsageTrackingService(usageRepository, subscriptionRepository, planRepository);

        plan = new SubscriptionPlan(
                "pro", "Pro Plan", "Desc", PlanTier.PROFESSIONAL,
                BillingInterval.MONTHLY, new BigDecimal("99.0000"), "USD",
                50, 20, 5000L, 25, 120, 500, 1000, 0
        );
        planId = plan.getId();

        sub = new OrganizationSubscription(
                orgId, planId, SubscriptionStatus.ACTIVE, BillingInterval.MONTHLY,
                Instant.now(), Instant.now().plusSeconds(3600), null
        );
    }

    @Test
    void testRecordUsage_IncrementExisting() {
        SubscriptionUsage existing = new SubscriptionUsage(
                orgId, UsageMetric.AI_REQUESTS, 10, Instant.now(), Instant.now().plusSeconds(3600));
        when(usageRepository.findByOrganizationIdAndMetric(orgId, UsageMetric.AI_REQUESTS))
                .thenReturn(Optional.of(existing));
        when(usageRepository.save(any(SubscriptionUsage.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionUsage result = service.recordUsage(orgId, UsageMetric.AI_REQUESTS, 5);
        assertEquals(15, result.getCurrentValue());
    }

    @Test
    void testSetUsage_Absolute() {
        SubscriptionUsage existing = new SubscriptionUsage(
                orgId, UsageMetric.MEMBERS, 10, Instant.now(), Instant.now().plusSeconds(3600));
        when(usageRepository.findByOrganizationIdAndMetric(orgId, UsageMetric.MEMBERS))
                .thenReturn(Optional.of(existing));
        when(usageRepository.save(any(SubscriptionUsage.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionUsage result = service.setUsage(orgId, UsageMetric.MEMBERS, 12);
        assertEquals(12, result.getCurrentValue());
    }

    @Test
    void testCheckEntitlement_WithinLimit() {
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        SubscriptionUsage existing = new SubscriptionUsage(
                orgId, UsageMetric.PROJECTS, 10, Instant.now(), Instant.now().plusSeconds(3600));
        when(usageRepository.findByOrganizationIdAndMetric(orgId, UsageMetric.PROJECTS))
                .thenReturn(Optional.of(existing));

        // Limit is 20, current is 10, delta is 5 -> 15 <= 20
        assertTrue(service.checkEntitlement(orgId, UsageMetric.PROJECTS, 5));
    }

    @Test
    void testCheckEntitlement_ExceedsLimit() {
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        SubscriptionUsage existing = new SubscriptionUsage(
                orgId, UsageMetric.PROJECTS, 18, Instant.now(), Instant.now().plusSeconds(3600));
        when(usageRepository.findByOrganizationIdAndMetric(orgId, UsageMetric.PROJECTS))
                .thenReturn(Optional.of(existing));

        // Limit is 20, current is 18, delta is 5 -> 23 > 20
        assertFalse(service.checkEntitlement(orgId, UsageMetric.PROJECTS, 5));
    }

    @Test
    void testGetRemainingQuota() {
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        SubscriptionUsage existing = new SubscriptionUsage(
                orgId, UsageMetric.AI_REQUESTS, 200, Instant.now(), Instant.now().plusSeconds(3600));
        when(usageRepository.findByOrganizationIdAndMetric(orgId, UsageMetric.AI_REQUESTS))
                .thenReturn(Optional.of(existing));

        // Limit is 500, current is 200 -> remaining 300
        assertEquals(300, service.getRemainingQuota(orgId, UsageMetric.AI_REQUESTS));
    }
}
