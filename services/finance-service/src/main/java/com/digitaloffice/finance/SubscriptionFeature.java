package com.digitaloffice.finance;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "subscription_features")
public class SubscriptionFeature {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID planId;

    @Column(nullable = false, length = 80)
    private String feature;

    @Column(nullable = false)
    private boolean enabled;

    protected SubscriptionFeature() {}

    public SubscriptionFeature(UUID planId, String feature, boolean enabled) {
        this.id = UUID.randomUUID();
        this.planId = planId;
        this.feature = feature;
        this.enabled = enabled;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public String getFeature() { return feature; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
