package com.digitaloffice.finance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationSubscriptionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrganizationSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationSubscriptionService subscriptionService;
    @MockBean
    private SubscriptionPlanService planService;
    @MockBean
    private FinanceAuthorizationService authService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();

    @Test
    void testGetSubscription_Success() throws Exception {
        SubscriptionPlan plan = new SubscriptionPlan(
                "pro", "Professional Plan", "Desc", PlanTier.PROFESSIONAL,
                BillingInterval.MONTHLY, new BigDecimal("99.0000"), "USD",
                50, 100, 50000L, 50, 240, 2500, 10000, 14
        );
        OrganizationSubscription sub = new OrganizationSubscription(
                orgId, planId, SubscriptionStatus.ACTIVE, BillingInterval.MONTHLY,
                Instant.now(), Instant.now().plusSeconds(86400), null
        );

        when(subscriptionService.getSubscription(orgId)).thenReturn(sub);
        when(planService.getPlan(sub.getPlanId())).thenReturn(plan);

        mockMvc.perform(get("/api/v1/organizations/{orgId}/subscription", orgId)
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(orgId.toString()))
                .andExpect(jsonPath("$.planName").value("pro"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(authService).requireMember(eq(orgId), any(), any());
    }

    @Test
    void testChangePlan_AccessDenied() throws Exception {
        doThrow(new AccessDeniedException("Organization owner or administrator permission is required"))
                .when(authService).requireFinanceAdministrator(eq(orgId), any(), any());

        String json = """
                {
                    "newPlanId": "%s"
                }
                """.formatted(planId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/change-plan", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isForbidden());
    }
}

