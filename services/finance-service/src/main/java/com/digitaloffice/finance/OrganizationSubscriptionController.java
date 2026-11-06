package com.digitaloffice.finance;

import com.digitaloffice.finance.dto.ChangePlanRequest;
import com.digitaloffice.finance.dto.CreateSubscriptionRequest;
import com.digitaloffice.finance.dto.SubscriptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/subscription")
public class OrganizationSubscriptionController {

    private final OrganizationSubscriptionService subscriptionService;
    private final SubscriptionPlanService planService;
    private final FinanceAuthorizationService authService;

    public OrganizationSubscriptionController(OrganizationSubscriptionService subscriptionService,
                                              SubscriptionPlanService planService,
                                              FinanceAuthorizationService authService) {
        this.subscriptionService = subscriptionService;
        this.planService = planService;
        this.authService = authService;
    }

    @GetMapping
    public SubscriptionResponse getSubscription(@PathVariable UUID organizationId,
                                                @AuthenticationPrincipal Jwt jwt,
                                                HttpServletRequest request) {
        authService.requireMember(organizationId, actor(jwt), token(request));
        OrganizationSubscription sub = subscriptionService.getSubscription(organizationId);
        SubscriptionPlan plan = planService.getPlan(sub.getPlanId());
        return SubscriptionResponse.from(sub, plan);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse createSubscription(@PathVariable UUID organizationId,
                                                   @Valid @RequestBody CreateSubscriptionRequest body,
                                                   @AuthenticationPrincipal Jwt jwt,
                                                   HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        OrganizationSubscription sub = subscriptionService.createSubscription(
                organizationId, body.planId(), body.billingInterval(), actor(jwt));
        SubscriptionPlan plan = planService.getPlan(sub.getPlanId());
        return SubscriptionResponse.from(sub, plan);
    }

    @PostMapping("/change-plan")
    public SubscriptionResponse changePlan(@PathVariable UUID organizationId,
                                           @Valid @RequestBody ChangePlanRequest body,
                                           @AuthenticationPrincipal Jwt jwt,
                                           HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        OrganizationSubscription sub = subscriptionService.changePlan(organizationId, body.newPlanId(), actor(jwt));
        SubscriptionPlan plan = planService.getPlan(sub.getPlanId());
        return SubscriptionResponse.from(sub, plan);
    }

    @PostMapping("/cancel")
    public SubscriptionResponse cancelSubscription(@PathVariable UUID organizationId,
                                                   @AuthenticationPrincipal Jwt jwt,
                                                   HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        OrganizationSubscription sub = subscriptionService.cancelSubscription(organizationId, actor(jwt));
        SubscriptionPlan plan = planService.getPlan(sub.getPlanId());
        return SubscriptionResponse.from(sub, plan);
    }

    @PostMapping("/renew")
    public SubscriptionResponse renewSubscription(@PathVariable UUID organizationId,
                                                  @AuthenticationPrincipal Jwt jwt,
                                                  HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        OrganizationSubscription sub = subscriptionService.renewSubscription(organizationId, actor(jwt));
        SubscriptionPlan plan = planService.getPlan(sub.getPlanId());
        return SubscriptionResponse.from(sub, plan);
    }

    @PostMapping("/reactivate")
    public SubscriptionResponse reactivateSubscription(@PathVariable UUID organizationId,
                                                       @AuthenticationPrincipal Jwt jwt,
                                                       HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        OrganizationSubscription sub = subscriptionService.reactivateSubscription(organizationId, actor(jwt));
        SubscriptionPlan plan = planService.getPlan(sub.getPlanId());
        return SubscriptionResponse.from(sub, plan);
    }

    @GetMapping("/history")
    public Page<SubscriptionHistory> getHistory(@PathVariable UUID organizationId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @AuthenticationPrincipal Jwt jwt,
                                                HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));
        return subscriptionService.getHistory(organizationId, page, size);
    }

    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
    }

    private static String token(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }
}

