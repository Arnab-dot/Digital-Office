package com.digitaloffice.finance;

import com.digitaloffice.finance.dto.RecordUsageRequest;
import com.digitaloffice.finance.dto.UsageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/usage")
public class UsageTrackingController {

    private final UsageTrackingService usageService;
    private final OrganizationSubscriptionService subscriptionService;
    private final SubscriptionPlanService planService;
    private final FinanceAuthorizationService authService;

    public UsageTrackingController(UsageTrackingService usageService,
                                   OrganizationSubscriptionService subscriptionService,
                                   SubscriptionPlanService planService,
                                   FinanceAuthorizationService authService) {
        this.usageService = usageService;
        this.subscriptionService = subscriptionService;
        this.planService = planService;
        this.authService = authService;
    }

    @GetMapping
    public List<UsageResponse> getUsage(@PathVariable UUID organizationId,
                                        @AuthenticationPrincipal Jwt jwt,
                                        HttpServletRequest request) {
        authService.requireMember(organizationId, actor(jwt), token(request));

        SubscriptionPlan plan = null;
        try {
            OrganizationSubscription sub = subscriptionService.getSubscription(organizationId);
            plan = planService.getPlan(sub.getPlanId());
        } catch (Exception ignored) {
            // Unsubscribed orgs use free defaults
        }

        Map<UsageMetric, SubscriptionUsage> tracked = new EnumMap<>(UsageMetric.class);
        for (SubscriptionUsage u : usageService.getUsage(organizationId)) {
            tracked.put(u.getMetric(), u);
        }

        Instant now = Instant.now();
        List<UsageResponse> responses = new ArrayList<>();
        for (UsageMetric metric : UsageMetric.values()) {
            SubscriptionUsage usage = tracked.get(metric);
            long current = usage != null ? usage.getCurrentValue() : 0L;
            long limit = plan != null ? getMetricLimit(plan, metric) : 0L;
            long remaining = usageService.getRemainingQuota(organizationId, metric);
            Instant periodStart = usage != null ? usage.getPeriodStart() : now.truncatedTo(ChronoUnit.DAYS);
            Instant periodEnd = usage != null ? usage.getPeriodEnd() : now.plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

            responses.add(new UsageResponse(metric, current, limit, remaining, periodStart, periodEnd));
        }

        return responses;
    }

    @PostMapping("/record")
    public UsageResponse recordUsage(@PathVariable UUID organizationId,
                                     @Valid @RequestBody RecordUsageRequest body,
                                     @AuthenticationPrincipal Jwt jwt,
                                     HttpServletRequest request) {
        authService.requireFinanceAdministrator(organizationId, actor(jwt), token(request));

        SubscriptionUsage usage;
        if (body.setAbsolute()) {
            usage = usageService.setUsage(organizationId, body.metric(), body.value());
        } else {
            usage = usageService.recordUsage(organizationId, body.metric(), body.value());
        }

        long remaining = usageService.getRemainingQuota(organizationId, body.metric());
        return new UsageResponse(
                usage.getMetric(),
                usage.getCurrentValue(),
                0L,
                remaining,
                usage.getPeriodStart(),
                usage.getPeriodEnd()
        );
    }

    @GetMapping("/check")
    public Map<String, Object> checkEntitlement(@PathVariable UUID organizationId,
                                                @RequestParam UsageMetric metric,
                                                @RequestParam(defaultValue = "1") long delta,
                                                @AuthenticationPrincipal Jwt jwt,
                                                HttpServletRequest request) {
        authService.requireMember(organizationId, actor(jwt), token(request));
        boolean allowed = usageService.checkEntitlement(organizationId, metric, delta);
        long remaining = usageService.getRemainingQuota(organizationId, metric);

        return Map.of(
                "metric", metric,
                "delta", delta,
                "allowed", allowed,
                "remainingQuota", remaining
        );
    }

    private long getMetricLimit(SubscriptionPlan plan, UsageMetric metric) {
        return switch (metric) {
            case MEMBERS -> plan.getMaxMembers();
            case STORAGE_MB -> plan.getStorageLimitMb();
            case PROJECTS -> plan.getMaxProjects();
            case MEETINGS -> 0L;
            case MEETING_MINUTES -> (long) plan.getMaxMeetingDurationMin() * plan.getMaxMeetingParticipants();
            case AI_REQUESTS -> plan.getAiRequestsLimit();
            case DOCUMENTS -> plan.getDocumentLimit();
            case API_CALLS -> 0L;
        };
    }

    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
    }

    private static String token(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }
}

