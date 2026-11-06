package com.digitaloffice.finance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_subscriptions")
public class OrganizationSubscription {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingInterval billingInterval;

    @Column(nullable = false)
    private Instant currentPeriodStart;

    @Column(nullable = false)
    private Instant currentPeriodEnd;

    private Instant trialEnd;

    private Instant cancelledAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private int version;

    protected OrganizationSubscription() {}

    public OrganizationSubscription(UUID organizationId, UUID planId, SubscriptionStatus status,
                                    BillingInterval billingInterval,
                                    Instant periodStart, Instant periodEnd, Instant trialEnd) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.planId = planId;
        this.status = status;
        this.billingInterval = billingInterval;
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.trialEnd = trialEnd;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getPlanId() { return planId; }
    public SubscriptionStatus getStatus() { return status; }
    public BillingInterval getBillingInterval() { return billingInterval; }
    public Instant getCurrentPeriodStart() { return currentPeriodStart; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public Instant getTrialEnd() { return trialEnd; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    // State transitions
    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        this.status = SubscriptionStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void markPastDue() {
        this.status = SubscriptionStatus.PAST_DUE;
        this.updatedAt = Instant.now();
    }

    public void changePlan(UUID newPlanId) {
        this.planId = newPlanId;
        this.updatedAt = Instant.now();
    }

    public void renew(Instant newPeriodStart, Instant newPeriodEnd) {
        this.currentPeriodStart = newPeriodStart;
        this.currentPeriodEnd = newPeriodEnd;
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void reactivate() {
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }
}
