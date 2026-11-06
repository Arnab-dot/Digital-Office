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
class OrganizationSubscriptionServiceTest {

    @Mock
    private OrganizationSubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPlanRepository planRepository;
    @Mock
    private SubscriptionHistoryRepository historyRepository;
    @Mock
    private SubscriptionUsageRepository usageRepository;

    private OrganizationSubscriptionService service;

    private final UUID orgId = UUID.randomUUID();
    private UUID starterPlanId;
    private UUID proPlanId;
    private final UUID actorId = UUID.randomUUID();

    private SubscriptionPlan starterPlan;
    private SubscriptionPlan proPlan;

    @BeforeEach
    void setUp() {
        service = new OrganizationSubscriptionService(
                subscriptionRepository, planRepository, historyRepository, usageRepository);

        starterPlan = new SubscriptionPlan(
                "starter", "Starter Plan", "Starter desc", PlanTier.STARTER,
                BillingInterval.MONTHLY, new BigDecimal("29.0000"), "USD",
                10, 5, 2000L, 20, 60, 100, 500, 14
        );
        starterPlanId = starterPlan.getId();

        proPlan = new SubscriptionPlan(
                "pro", "Pro Plan", "Pro desc", PlanTier.PROFESSIONAL,
                BillingInterval.MONTHLY, new BigDecimal("99.0000"), "USD",
                50, 25, 10000L, 50, 120, 1000, 5000, 0
        );
        proPlanId = proPlan.getId();
    }

    @Test
    void testCreateSubscription_Success() {
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.empty());
        when(planRepository.findById(proPlanId)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.save(any(OrganizationSubscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        OrganizationSubscription result = service.createSubscription(orgId, proPlanId, BillingInterval.MONTHLY, actorId);

        assertNotNull(result);
        assertEquals(orgId, result.getOrganizationId());
        assertEquals(proPlanId, result.getPlanId());
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        verify(historyRepository).save(any(SubscriptionHistory.class));
    }

    @Test
    void testCreateSubscription_WithTrial() {
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.empty());
        when(planRepository.findById(starterPlanId)).thenReturn(Optional.of(starterPlan));
        when(subscriptionRepository.save(any(OrganizationSubscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        OrganizationSubscription result = service.createSubscription(orgId, starterPlanId, BillingInterval.MONTHLY, actorId);

        assertNotNull(result);
        assertEquals(SubscriptionStatus.TRIAL, result.getStatus());
        assertNotNull(result.getTrialEnd());
    }

    @Test
    void testCreateSubscription_ThrowsWhenAlreadySubscribed() {
        OrganizationSubscription existing = new OrganizationSubscription(
                orgId, proPlanId, SubscriptionStatus.ACTIVE, BillingInterval.MONTHLY,
                Instant.now(), Instant.now().plusSeconds(3600), null
        );
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () ->
                service.createSubscription(orgId, proPlanId, BillingInterval.MONTHLY, actorId));
    }

    @Test
    void testChangePlan_Upgrade() {
        OrganizationSubscription sub = new OrganizationSubscription(
                orgId, starterPlanId, SubscriptionStatus.ACTIVE, BillingInterval.MONTHLY,
                Instant.now(), Instant.now().plusSeconds(3600), null
        );

        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));
        when(planRepository.findById(proPlanId)).thenReturn(Optional.of(proPlan));
        when(planRepository.findById(starterPlanId)).thenReturn(Optional.of(starterPlan));
        when(subscriptionRepository.save(any(OrganizationSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationSubscription result = service.changePlan(orgId, proPlanId, actorId);

        assertEquals(proPlanId, result.getPlanId());
        verify(historyRepository).save(argThat(h -> h.getAction() == SubscriptionAction.UPGRADED));
    }

    @Test
    void testChangePlan_DowngradeBlockedWhenUsageExceedsLimit() {
        OrganizationSubscription sub = new OrganizationSubscription(
                orgId, proPlanId, SubscriptionStatus.ACTIVE, BillingInterval.MONTHLY,
                Instant.now(), Instant.now().plusSeconds(3600), null
        );

        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));
        when(planRepository.findById(starterPlanId)).thenReturn(Optional.of(starterPlan));
        when(planRepository.findById(proPlanId)).thenReturn(Optional.of(proPlan));

        // Organization has 25 active members, but starterPlan maxMembers is 10
        SubscriptionUsage memberUsage = new SubscriptionUsage(
                orgId, UsageMetric.MEMBERS, 25, Instant.now(), Instant.now().plusSeconds(3600));
        when(usageRepository.findByOrganizationIdAndMetric(orgId, UsageMetric.MEMBERS))
                .thenReturn(Optional.of(memberUsage));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.changePlan(orgId, starterPlanId, actorId));

        assertTrue(ex.getMessage().contains("Cannot downgrade plan"));
        assertTrue(ex.getMessage().contains("active members (25)"));
        verify(subscriptionRepository, never()).save(sub);
    }

    @Test
    void testCancelSubscription() {
        OrganizationSubscription sub = new OrganizationSubscription(
                orgId, proPlanId, SubscriptionStatus.ACTIVE, BillingInterval.MONTHLY,
                Instant.now(), Instant.now().plusSeconds(3600), null
        );

        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganizationSubscription result = service.cancelSubscription(orgId, actorId);
        assertEquals(SubscriptionStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
        verify(historyRepository).save(argThat(h -> h.getAction() == SubscriptionAction.CANCELLED));
    }
}
