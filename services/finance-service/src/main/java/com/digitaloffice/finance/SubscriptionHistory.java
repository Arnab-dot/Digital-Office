package com.digitaloffice.finance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_history")
public class SubscriptionHistory {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionAction action;

    private UUID fromPlanId;

    private UUID toPlanId;

    @Column(nullable = false)
    private UUID actorId;

    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected SubscriptionHistory() {}

    public SubscriptionHistory(UUID subscriptionId, UUID organizationId, SubscriptionAction action,
                               UUID fromPlanId, UUID toPlanId, UUID actorId, String note) {
        this.id = UUID.randomUUID();
        this.subscriptionId = subscriptionId;
        this.organizationId = organizationId;
        this.action = action;
        this.fromPlanId = fromPlanId;
        this.toPlanId = toPlanId;
        this.actorId = actorId;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public UUID getOrganizationId() { return organizationId; }
    public SubscriptionAction getAction() { return action; }
    public UUID getFromPlanId() { return fromPlanId; }
    public UUID getToPlanId() { return toPlanId; }
    public UUID getActorId() { return actorId; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
