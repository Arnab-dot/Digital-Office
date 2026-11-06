package com.digitaloffice.work;

import com.digitaloffice.contracts.MembershipSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * REST controller for workstream and milestone operations.
 *
 * <p>Authorization follows Requirements 1.3 and 1.9:
 * {@code list()} for workstreams and milestones returns all records in the project
 * when the actor's organization holds an ACTIVE participation record.
 * Mutations (create/update/archive/status) are restricted to the resource creator.
 */
@RestController
@RequestMapping("/api/v1")
public class WorkstreamController {

    private final WorkstreamRepository workstreams;
    private final MilestoneRepository milestones;
    private final OrganizationMembershipClient membershipClient;
    private final ProjectParticipationClient participationClient;

    public WorkstreamController(WorkstreamRepository workstreams,
                                MilestoneRepository milestones,
                                OrganizationMembershipClient membershipClient,
                                ProjectParticipationClient participationClient) {
        this.workstreams = workstreams;
        this.milestones = milestones;
        this.membershipClient = membershipClient;
        this.participationClient = participationClient;
    }

    // =========================================================================
    // Workstream endpoints
    // =========================================================================

    /**
     * Create a workstream in the project.
     * Requires actor's org to have ACTIVE participation in the project.
     */
    @PostMapping("/projects/{projectId}/workstreams")
    public Workstream create(@PathVariable UUID projectId,
                             @AuthenticationPrincipal Jwt jwt,
                             @RequestHeader("Authorization") String authHeader,
                             @Valid @RequestBody WorkstreamRequest r) {
        UUID actor = user(jwt);
        requireOrgParticipation(projectId, actor, authHeader);
        return workstreams.save(new Workstream(projectId, r.name(), r.description(), actor));
    }

    /**
     * List ACTIVE workstreams for the project.
     * Requirement 1.9: return all workstreams in projects where actor's org participates.
     * Filter removed from {@code createdBy} to participation-based access.
     */
    @GetMapping("/projects/{projectId}/workstreams")
    public List<Workstream> list(@PathVariable UUID projectId,
                                 @AuthenticationPrincipal Jwt jwt,
                                 @RequestHeader("Authorization") String authHeader) {
        UUID actor = user(jwt);
        requireOrgParticipation(projectId, actor, authHeader);   // Req 1.3 / 1.9
        return workstreams.findByProjectIdAndStatus(projectId, WorkstreamStatus.ACTIVE);
    }

    /**
     * Update a workstream — restricted to the creator.
     */
    @PatchMapping("/workstreams/{id}")
    public Workstream update(@PathVariable UUID id,
                             @AuthenticationPrincipal Jwt jwt,
                             @RequestHeader("Authorization") String authHeader,
                             @Valid @RequestBody WorkstreamRequest r) {
        UUID actor = user(jwt);
        Workstream w = ownedWorkstream(id, actor);
        w.update(r.name(), r.description());
        return workstreams.save(w);
    }

    /**
     * Archive a workstream — restricted to the creator.
     */
    @PostMapping("/workstreams/{id}/archive")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id,
                        @AuthenticationPrincipal Jwt jwt,
                        @RequestHeader("Authorization") String authHeader) {
        UUID actor = user(jwt);
        Workstream w = ownedWorkstream(id, actor);
        w.archive();
        workstreams.save(w);
    }

    // =========================================================================
    // Milestone endpoints
    // =========================================================================

    /**
     * Create a milestone in the project.
     * Requires actor's org to have ACTIVE participation in the project.
     */
    @PostMapping("/projects/{projectId}/milestones")
    public Milestone createMilestone(@PathVariable UUID projectId,
                                     @AuthenticationPrincipal Jwt jwt,
                                     @RequestHeader("Authorization") String authHeader,
                                     @Valid @RequestBody MilestoneRequest r) {
        UUID actor = user(jwt);
        requireOrgParticipation(projectId, actor, authHeader);
        if (r.workstreamId() != null) {
            Workstream w = workstreams.findById(r.workstreamId()).orElseThrow();
            if (!w.getProjectId().equals(projectId)) {
                throw new IllegalArgumentException("Workstream belongs to another project");
            }
        }
        return milestones.save(new Milestone(projectId, r.workstreamId(), r.title(), r.description(), r.dueDate(), actor));
    }

    /**
     * List milestones for the project.
     * Requirement 1.9: return all milestones in projects where actor's org participates.
     * Filter removed from {@code createdBy} to participation-based access.
     */
    @GetMapping("/projects/{projectId}/milestones")
    public List<Milestone> milestones(@PathVariable UUID projectId,
                                      @AuthenticationPrincipal Jwt jwt,
                                      @RequestHeader("Authorization") String authHeader) {
        UUID actor = user(jwt);
        requireOrgParticipation(projectId, actor, authHeader);   // Req 1.3 / 1.9
        return milestones.findByProjectId(projectId);             // Req 1.9 – return all
    }

    /**
     * Update a milestone — restricted to the creator.
     */
    @PatchMapping("/milestones/{id}")
    public Milestone updateMilestone(@PathVariable UUID id,
                                     @AuthenticationPrincipal Jwt jwt,
                                     @RequestHeader("Authorization") String authHeader,
                                     @Valid @RequestBody MilestoneRequest r) {
        Milestone m = ownedMilestone(id, user(jwt));
        m.update(r.title(), r.description(), r.dueDate());
        return milestones.save(m);
    }

    /**
     * Transition a milestone status — restricted to the creator.
     */
    @PostMapping("/milestones/{id}/status")
    public Milestone status(@PathVariable UUID id,
                            @AuthenticationPrincipal Jwt jwt,
                            @RequestHeader("Authorization") String authHeader,
                            @RequestParam MilestoneStatus value) {
        Milestone m = ownedMilestone(id, user(jwt));
        m.transition(value);
        return milestones.save(m);
    }

    // =========================================================================
    // Authorization helpers
    // =========================================================================

    /**
     * Resolves actor's active org IDs and verifies at least one is an ACTIVE participant
     * of the given project.  Throws {@link AccessDeniedException} (→ HTTP 403) on failure.
     */
    private void requireOrgParticipation(UUID projectId, UUID actor, String authHeader) {
        List<MembershipSummary> memberships = membershipClient.getActiveMemberships(actor, authHeader);
        if (memberships == null || memberships.isEmpty()) {
            throw new AccessDeniedException("Actor has no active organization membership");
        }
        boolean orgParticipates = memberships.stream()
                .anyMatch(m -> participationClient.hasActiveParticipation(projectId, m.organizationId(), authHeader));
        if (!orgParticipates) {
            throw new AccessDeniedException("Actor's organization is not a participant in the project");
        }
    }

    private Workstream ownedWorkstream(UUID id, UUID actor) {
        Workstream w = workstreams.findById(id).orElseThrow();
        if (!w.getCreatedBy().equals(actor)) throw new AccessDeniedException("Workstream access denied");
        return w;
    }

    private Milestone ownedMilestone(UUID id, UUID actor) {
        Milestone m = milestones.findById(id).orElseThrow();
        if (!m.getCreatedBy().equals(actor)) throw new AccessDeniedException("Milestone access denied");
        return m;
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private static UUID user(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes());
    }

    // =========================================================================
    // Request records
    // =========================================================================

    public record WorkstreamRequest(@NotBlank @Size(max = 200) String name, String description) {}

    public record MilestoneRequest(@NotBlank @Size(max = 240) String title,
                                   String description,
                                   LocalDate dueDate,
                                   UUID workstreamId) {}
}
