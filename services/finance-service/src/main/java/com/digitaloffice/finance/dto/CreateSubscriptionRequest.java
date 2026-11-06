package com.digitaloffice.finance.dto;

import com.digitaloffice.finance.BillingInterval;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateSubscriptionRequest(
    @NotNull(message = "planId is required") UUID planId,
    @NotNull(message = "billingInterval is required") BillingInterval billingInterval
) {}

