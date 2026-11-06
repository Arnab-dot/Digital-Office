package com.digitaloffice.work;

import com.digitaloffice.contracts.MembershipSummary;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class ActivityController {
    private final ActivityService activity;
    private final TaskRepository tasks;
    private final OrganizationMembershipClient memberships;
    private final ProjectParticipationClient participations;
    private final DocumentActivityClient documentActivity;
    public ActivityController(ActivityService activity, TaskRepository tasks,
                              OrganizationMembershipClient memberships, ProjectParticipationClient participations,
                              DocumentActivityClient documentActivity) {
        this.activity = activity; this.tasks = tasks; this.memberships = memberships;
        this.participations = participations; this.documentActivity = documentActivity;
    }
    @GetMapping("/projects/{projectId}/activity")
    public Page<ActivityFeedItem> project(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt,
                          @RequestHeader("Authorization") String token,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        requireProject(projectId, actor(jwt), token);
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        List<ActivityFeedItem> items = new ArrayList<>(activity.findByProject(projectId).stream()
                .map(e -> new ActivityFeedItem(e.getId(), e.getProjectId(), e.getActorId(), e.getActionType().name(),
                        e.getSubjectId(), e.getSubjectType(), e.getMetadata(), e.getEventAt())).toList());
        items.addAll(documentActivity.forProject(projectId, token));
        items.sort(Comparator.comparing(ActivityFeedItem::eventAt).reversed());
        int from = (int) Math.min((long) page * size, items.size());
        int to = Math.min(from + size, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, size), items.size());
    }
    @GetMapping("/tasks/{taskId}/activity")
    public List<ActivityEvent> task(@PathVariable UUID taskId, @AuthenticationPrincipal Jwt jwt,
                                    @RequestHeader("Authorization") String token) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new NoSuchElementException("Task not found"));
        requireProject(task.getProjectId(), actor(jwt), token);
        return activity.findBySubject(taskId);
    }
    private void requireProject(UUID projectId, UUID actor, String token) {
        boolean allowed = memberships.getActiveMemberships(actor, token).stream()
                .anyMatch(m -> participations.hasActiveParticipation(projectId, m.organizationId(), token));
        if (!allowed) throw new AccessDeniedException("Actor cannot access this project");
    }
    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
    }
}
