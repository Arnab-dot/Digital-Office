package com.digitaloffice.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangePlanRequest(
    @NotNull(message = "newPlanId is required") UUID newPlanId
) {}

