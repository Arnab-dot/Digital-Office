package com.digitaloffice.finance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_usage")
public class SubscriptionUsage {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UsageMetric metric;

    @Column(nullable = false)
    private long currentValue;

    @Column(nullable = false)
    private Instant periodStart;

    @Column(nullable = false)
    private Instant periodEnd;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SubscriptionUsage() {}

    public SubscriptionUsage(UUID organizationId, UsageMetric metric, long currentValue,
                             Instant periodStart, Instant periodEnd) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.metric = metric;
        this.currentValue = currentValue;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UsageMetric getMetric() { return metric; }
    public long getCurrentValue() { return currentValue; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void incrementBy(long delta) {
        this.currentValue += delta;
        this.updatedAt = Instant.now();
    }

    public void setValue(long value) {
        this.currentValue = value;
        this.updatedAt = Instant.now();
    }
}
