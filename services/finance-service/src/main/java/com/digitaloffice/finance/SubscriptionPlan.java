package com.digitaloffice.finance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String displayName;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingInterval billingInterval;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int maxMembers;

    @Column(nullable = false)
    private int maxProjects;

    @Column(nullable = false)
    private long storageLimitMb;

    @Column(nullable = false)
    private int maxMeetingParticipants;

    @Column(nullable = false)
    private int maxMeetingDurationMin;

    @Column(nullable = false)
    private int aiRequestsLimit;

    @Column(nullable = false)
    private int documentLimit;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private int trialDays;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SubscriptionPlan() {}

    public SubscriptionPlan(String name, String displayName, String description, PlanTier tier,
                            BillingInterval billingInterval, BigDecimal price, String currency,
                            int maxMembers, int maxProjects, long storageLimitMb,
                            int maxMeetingParticipants, int maxMeetingDurationMin,
                            int aiRequestsLimit, int documentLimit, int trialDays) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.tier = tier;
        this.billingInterval = billingInterval;
        this.price = price;
        this.currency = currency;
        this.maxMembers = maxMembers;
        this.maxProjects = maxProjects;
        this.storageLimitMb = storageLimitMb;
        this.maxMeetingParticipants = maxMeetingParticipants;
        this.maxMeetingDurationMin = maxMeetingDurationMin;
        this.aiRequestsLimit = aiRequestsLimit;
        this.documentLimit = documentLimit;
        this.trialDays = trialDays;
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public PlanTier getTier() { return tier; }
    public BillingInterval getBillingInterval() { return billingInterval; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public int getMaxMembers() { return maxMembers; }
    public int getMaxProjects() { return maxProjects; }
    public long getStorageLimitMb() { return storageLimitMb; }
    public int getMaxMeetingParticipants() { return maxMeetingParticipants; }
    public int getMaxMeetingDurationMin() { return maxMeetingDurationMin; }
    public int getAiRequestsLimit() { return aiRequestsLimit; }
    public int getDocumentLimit() { return documentLimit; }
    public boolean isActive() { return isActive; }
    public int getTrialDays() { return trialDays; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Mutators
    public void update(String displayName, String description, BigDecimal price,
                       int maxMembers, int maxProjects, long storageLimitMb,
                       int maxMeetingParticipants, int maxMeetingDurationMin,
                       int aiRequestsLimit, int documentLimit) {
        this.displayName = displayName;
        this.description = description;
        this.price = price;
        this.maxMembers = maxMembers;
        this.maxProjects = maxProjects;
        this.storageLimitMb = storageLimitMb;
        this.maxMeetingParticipants = maxMeetingParticipants;
        this.maxMeetingDurationMin = maxMeetingDurationMin;
        this.aiRequestsLimit = aiRequestsLimit;
        this.documentLimit = documentLimit;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = Instant.now();
    }
}
