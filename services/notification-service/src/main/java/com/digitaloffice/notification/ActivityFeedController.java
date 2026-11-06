package com.digitaloffice.notification;

import com.digitaloffice.notification.dto.ActivityFeedResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ActivityFeedController {

    private final ActivityFeedService service;

    public ActivityFeedController(ActivityFeedService service) {
        this.service = service;
    }

    @GetMapping("/organizations/{organizationId}/activity")
    public Page<ActivityFeedResponse> getOrganizationActivity(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getOrganizationActivity(organizationId, category, page, size)
                .map(ActivityFeedResponse::from);
    }

    @GetMapping("/projects/{projectId}/activity")
    public Page<ActivityFeedResponse> getProjectActivity(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getProjectActivity(projectId, category, page, size)
                .map(ActivityFeedResponse::from);
    }
}

