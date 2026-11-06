package com.digitaloffice.work;

import com.digitaloffice.contracts.MembershipSummary;
import com.digitaloffice.contracts.DomainEventPublisher;
import com.digitaloffice.contracts.UserNotificationEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * REST controller for task operations.
 *
 * <p>Authorization is two-step per Requirements 1.3, 2.1–2.5:
 * <ol>
 *   <li>Actor's org must hold an ACTIVE participation in the task's project
 *       (resolved via {@link OrganizationMembershipClient} + {@link ProjectParticipationClient}).</li>
 *   <li>For assignee-specific mutations the actor may also act if they are the current assignee.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1")
public class TaskController {

    private final TaskRepository tasks;
    private final TaskDependencyService dependencyService;
    private final OrganizationMembershipClient membershipClient;
    private final ProjectParticipationClient participationClient;
    private final ActivityService activity;
    private final DomainEventPublisher domainEvents;

    public TaskController(TaskRepository tasks,
                          TaskDependencyService dependencyService,
                          OrganizationMembershipClient membershipClient,
                          ProjectParticipationClient participationClient,
                          ActivityService activity,
                          DomainEventPublisher domainEvents) {
        this.tasks = tasks;
        this.dependencyService = dependencyService;
        this.membershipClient = membershipClient;
        this.participationClient = participationClient;
        this.activity = activity;
        this.domainEvents = domainEvents;
    }

    // -------------------------------------------------------------------------
    // Collection endpoints
    // -------------------------------------------------------------------------

    /**
     * Create a task in the project.
     * Requirement 2.4: actor's org must be enrolled; otherwise 422 (IllegalArgumentException).
     */
    @PostMapping("/projects/{projectId}/tasks")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@PathVariable UUID projectId,
                       @AuthenticationPrincipal Jwt jwt,
                       @RequestHeader("Authorization") String authHeader,
                       @Valid @RequestBody TaskRequest r) {
        UUID actor = user(jwt);
        requireOrgParticipation(projectId, actor, authHeader, true);   // Req 2.4 / 2.5
        Task saved = tasks.save(new Task(projectId, r.title(), r.description(), r.priority(), r.dueDate(), actor));
        activity.recordEvent(projectId, actor, WorkActivityActionType.TASK_CREATED, saved.getId(), "TASK", null);
        return saved;
    }

    /**
     * List all tasks in the project visible to the actor.
     * Requirement 2.3: return ALL tasks in the project when actor's org participates.
     */
    @GetMapping("/projects/{projectId}/tasks")
    public List<Task> list(@PathVariable UUID projectId,
                           @AuthenticationPrincipal Jwt jwt,
                           @RequestHeader("Authorization") String authHeader) {
        UUID actor = user(jwt);
        requireOrgParticipation(projectId, actor, authHeader, false);   // Req 1.3
        return tasks.findByProjectId(projectId);                  // Req 2.3 – return all
    }

    // -------------------------------------------------------------------------
    // Single-resource endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/tasks/{id}")
    public Task get(@PathVariable UUID id,
                    @AuthenticationPrincipal Jwt jwt,
                    @RequestHeader("Authorization") String authHeader) {
        return requireTaskAccess(id, user(jwt), authHeader);
    }

    @PatchMapping("/tasks/{id}")
    @Transactional
    public Task update(@PathVariable UUID id,
                       @AuthenticationPrincipal Jwt jwt,
                       @RequestHeader("Authorization") String authHeader,
                       @Valid @RequestBody TaskRequest r) {
        Task t = requireTaskAccess(id, user(jwt), authHeader);
        requireParticipantOrCreator(t, user(jwt), authHeader);
        t.update(r.title(), r.description(), r.priority(), r.dueDate());
        Task saved = tasks.save(t);
        activity.recordEvent(t.getProjectId(), user(jwt), WorkActivityActionType.TASK_UPDATED, id, "TASK", "{\"fields\":\"title,description,priority,dueDate\"}");
        return saved;
    }

    @PostMapping("/tasks/{id}/status")
    @Transactional
    public Task status(@PathVariable UUID id,
                       @AuthenticationPrincipal Jwt jwt,
                       @RequestHeader("Authorization") String authHeader,
                       @RequestParam TaskStatus value) {
        Task t = requireTaskAccess(id, user(jwt), authHeader);
        t.transition(value);
        Task saved = tasks.save(t);
        activity.recordEvent(t.getProjectId(), user(jwt), WorkActivityActionType.TASK_STATUS_CHANGED, id, "TASK", "{\"status\":\"" + value + "\"}");
        return saved;
    }

    @PostMapping("/tasks/{id}/assign")
    @Transactional
    public Task assign(@PathVariable UUID id,
                       @AuthenticationPrincipal Jwt jwt,
                       @RequestHeader("Authorization") String authHeader,
                       @RequestParam UUID userId) {
        Task t = requireTaskAccess(id, user(jwt), authHeader);
        requireParticipantOrCreator(t, user(jwt), authHeader);
        t.assign(userId);
        Task saved = tasks.save(t);
        activity.recordEvent(t.getProjectId(), user(jwt), WorkActivityActionType.TASK_ASSIGNED, id, "TASK", "{\"assigneeId\":\"" + userId + "\"}");
        domainEvents.publish(new UserNotificationEvent("TASK_ASSIGNED", id, user(jwt), java.time.Instant.now(),
                userId, "TASK", "{\"projectId\":\"" + t.getProjectId() + "\"}"));
        return saved;
    }

    // -------------------------------------------------------------------------
    // Dependency sub-resource
    // -------------------------------------------------------------------------

    @PostMapping("/tasks/{id}/dependencies")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDependency addDependency(@PathVariable UUID id,
                                        @AuthenticationPrincipal Jwt jwt,
                                        @RequestHeader("Authorization") String authHeader,
                                        @Valid @RequestBody DependencyRequest r) {
        UUID actor = user(jwt);
        Task t = tasks.findById(id).orElseThrow();
        requireOrgParticipation(t.getProjectId(), actor, authHeader, false);
        return dependencyService.add(id, r.dependsOnTaskId(), actor);
    }

    @GetMapping("/tasks/{id}/dependencies")
    public List<TaskDependency> dependencies(@PathVariable UUID id,
                                             @AuthenticationPrincipal Jwt jwt,
                                             @RequestHeader("Authorization") String authHeader) {
        UUID actor = user(jwt);
        Task t = tasks.findById(id).orElseThrow();
        requireOrgParticipation(t.getProjectId(), actor, authHeader, false);
        return dependencyService.list(id, actor);
    }

    @DeleteMapping("/tasks/{id}/dependencies/{dependencyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDependency(@PathVariable UUID id,
                                 @PathVariable UUID dependencyId,
                                 @AuthenticationPrincipal Jwt jwt,
                                 @RequestHeader("Authorization") String authHeader) {
        UUID actor = user(jwt);
        Task t = tasks.findById(id).orElseThrow();
        requireOrgParticipation(t.getProjectId(), actor, authHeader, false);
        dependencyService.remove(id, dependencyId, actor);
    }

    // -------------------------------------------------------------------------
    // Authorization helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves actor's active org IDs and verifies at least one is an ACTIVE participant
     * of the given project.  Throws {@link IllegalArgumentException} (→ HTTP 422) per
     * Requirement 2.4 when called from create(), or {@link AccessDeniedException} (→ HTTP 403)
     * for all other access paths.
     *
     * <p>This method is also used from create() — callers that need the 422 semantic for
     * "org not enrolled" should use the dedicated {@link #requireOrgParticipation} variant.
     */
    /**
     * Requires the actor's org to hold ACTIVE participation in the project.
     * Throws {@link IllegalArgumentException} (HTTP 422) when the actor's org is not enrolled —
     * used for create() per Requirement 2.4.
     */
    private void requireOrgParticipation(UUID projectId, UUID actor, String authHeader, boolean creating) {
        List<MembershipSummary> memberships = membershipClient.getActiveMemberships(actor, authHeader);
        if (memberships == null || memberships.isEmpty()) {
            if (creating) throw new IllegalArgumentException("Actor's organization is not a participant in the project");
            throw new AccessDeniedException("Actor has no active organization membership");
        }
        boolean orgParticipates = memberships.stream()
                .anyMatch(m -> participationClient.hasActiveParticipation(projectId, m.organizationId(), authHeader));
        if (!orgParticipates) {
            if (creating) throw new IllegalArgumentException("Actor's organization is not a participant in the project");
            throw new AccessDeniedException("Actor's organization is not a participant in the project");
        }
    }

    /**
     * Loads the task and asserts the actor may access it.
     * Two-step: (1) org participates in the task's project OR (2) actor is the assignee.
     */
    private Task requireTaskAccess(UUID taskId, UUID actor, String authHeader) {
        Task t = tasks.findById(taskId).orElseThrow();
        if (actor.equals(t.getCreatedBy()) || actor.equals(t.getAssigneeId())) return t;
        requireOrgParticipation(t.getProjectId(), actor, authHeader, false);
        return t;
    }

    private void requireParticipantOrCreator(Task task, UUID actor, String authHeader) {
        if (actor.equals(task.getCreatedBy())) return;
        if (actor.equals(task.getAssigneeId()))
            throw new AccessDeniedException("Assignees may update task status and comments only");
        requireOrgParticipation(task.getProjectId(), actor, authHeader, false);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static UUID user(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes());
    }

    // -------------------------------------------------------------------------
    // Request records
    // -------------------------------------------------------------------------

    public record TaskRequest(
            @NotBlank @Size(max = 240) String title,
            String description,
            @NotBlank String priority,
            LocalDate dueDate) {}

    public record DependencyRequest(@NotNull UUID dependsOnTaskId) {}
}
