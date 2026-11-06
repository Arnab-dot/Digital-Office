package com.digitaloffice.finance;

import com.digitaloffice.finance.dto.SubscriptionPlanResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/plans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    public SubscriptionPlanController(SubscriptionPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<SubscriptionPlanResponse> listActivePlans() {
        return planService.listActivePlans().stream()
                .map(p -> SubscriptionPlanResponse.from(p, planService.getFeatures(p.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public SubscriptionPlanResponse getPlan(@PathVariable UUID id) {
        SubscriptionPlan plan = planService.getPlan(id);
        return SubscriptionPlanResponse.from(plan, planService.getFeatures(plan.getId()));
    }
}

