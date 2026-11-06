package com.digitaloffice.finance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionPlanController.class)
@Import(SecurityConfig.class)
class SubscriptionPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionPlanService planService;

    @Test
    void testListActivePlans_Authenticated() throws Exception {
        SubscriptionPlan plan = new SubscriptionPlan(
                "pro", "Professional Plan", "Pro description", PlanTier.PROFESSIONAL,
                BillingInterval.MONTHLY, new BigDecimal("99.0000"), "USD",
                50, 100, 50000L, 50, 240, 2500, 10000, 14
        );
        SubscriptionFeature feat = new SubscriptionFeature(plan.getId(), "ADVANCED_SEARCH", true);

        when(planService.listActivePlans()).thenReturn(List.of(plan));
        when(planService.getFeatures(plan.getId())).thenReturn(List.of(feat));

        mockMvc.perform(get("/api/v1/finance/plans")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("pro"))
                .andExpect(jsonPath("$[0].tier").value("PROFESSIONAL"))
                .andExpect(jsonPath("$[0].features[0]").value("ADVANCED_SEARCH"));
    }

    @Test
    void testListActivePlans_UnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/finance/plans"))
                .andExpect(status().isUnauthorized());
    }
}

