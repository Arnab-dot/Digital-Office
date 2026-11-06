package com.digitaloffice.project;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for project management.
 *
 * <p>All business logic and authorization has been extracted to {@link ProjectService}.
 * This controller is responsible only for HTTP concerns: reading the JWT, extracting
 * the bearer token from the {@code Authorization} header, and delegating to the service.
 */
@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectInvitationService invitationService;

    public ProjectController(ProjectService projectService,
                             ProjectInvitationService invitationService) {
        this.projectService = projectService;
        this.invitationService = invitationService;
    }

    // -----------------------------------------------------------------------
    // Project CRUD
    // -----------------------------------------------------------------------

    /**
     * Creates a new project. Validates that {@code ownerOrganizationId} is an org
     * where the actor holds an ACTIVE membership (Requirement 1.2).
     */
    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@AuthenticationPrincipal Jwt jwt,
                          @Valid @RequestBody ProjectRequest r,
                          HttpServletRequest request) {
        return projectService.create(
                r.name(), r.description(), r.ownerOrganizationId(), r.clientId(),
                actor(jwt), bearerToken(request));
    }

    /**
     * Lists all projects accessible to the actor: projects where the actor's org has
     * ACTIVE participation, or the actor is the project creator (Requirement 1.8).
     *
     * <p>The {@code organizationId} request parameter is now optional and ignored —
     * access is derived from the actor's JWT identity via Organization_Service.
     */
    @GetMapping("/projects")
    public List<Project> list(@AuthenticationPrincipal Jwt jwt,
                              HttpServletRequest request) {
        return projectService.list(actor(jwt), bearerToken(request));
    }

    /**
     * Returns a single project. Verifies actor's org has ACTIVE participation or
     * actor is the project creator (Requirement 1.2).
     */
    @GetMapping("/projects/{id}")
    public Project get(@PathVariable UUID id,
                       @AuthenticationPrincipal Jwt jwt,
                       HttpServletRequest request) {
        return projectService.get(id, actor(jwt), bearerToken(request));
    }

    /**
     * Updates a project. Same access guard as {@link #get}.
     */
    @PatchMapping("/projects/{id}")
    public Project update(@PathVariable UUID id,
                          @AuthenticationPrincipal Jwt jwt,
                          @Valid @RequestBody ProjectRequest r,
                          HttpServletRequest request) {
        return projectService.update(id, r.name(), r.description(), actor(jwt), bearerToken(request));
    }

    /**
     * Closes a project. Same access guard as {@link #get}.
     */
    @PostMapping("/projects/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable UUID id,
                      @AuthenticationPrincipal Jwt jwt,
                      HttpServletRequest request) {
        projectService.close(id, actor(jwt), bearerToken(request));
    }

    /**
     * Returns the list of participants. Same access guard as {@link #get}.
     */
    @GetMapping("/projects/{id}/participants")
    public List<ProjectParticipation> participants(@PathVariable UUID id,
                                                   @AuthenticationPrincipal Jwt jwt,
                                                   HttpServletRequest request) {
        return projectService.participants(id, actor(jwt), bearerToken(request));
    }

    // -----------------------------------------------------------------------
    // Invitation endpoints (delegation to ProjectInvitationService, unchanged)
    // -----------------------------------------------------------------------

    @PostMapping("/projects/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectInvitation invite(@PathVariable UUID id,
                                    @AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody InvitationRequest r) {
        return invitationService.invite(id, r.organizationId(), r.recipientUserId(), actor(jwt));
    }

    @GetMapping("/projects/{id}/invitations")
    public List<ProjectInvitation> invitations(@PathVariable UUID id,
                                               @AuthenticationPrincipal Jwt jwt) {
        return invitationService.forProject(id, actor(jwt));
    }

    @PostMapping("/project-invitations/{id}/accept")
    public ProjectParticipation accept(@PathVariable UUID id,
                                       @AuthenticationPrincipal Jwt jwt) {
        return invitationService.resolve(id, actor(jwt), true);
    }

    @PostMapping("/project-invitations/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID id,
                       @AuthenticationPrincipal Jwt jwt) {
        invitationService.resolve(id, actor(jwt), false);
    }

    @PostMapping("/project-invitations/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id,
                       @AuthenticationPrincipal Jwt jwt) {
        invitationService.revoke(id, actor(jwt));
    }

    @GetMapping("/project-invitations/pending")
    public List<ProjectInvitation> pendingInvitations(@AuthenticationPrincipal Jwt jwt) {
        return invitationService.pendingFor(actor(jwt));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Derives the actor's internal UUID from the JWT {@code sub} claim. */
    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes());
    }

    /**
     * Extracts the raw {@code Authorization} header value (including the {@code Bearer }
     * prefix) so it can be forwarded to downstream service calls.
     */
    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null ? header : "";
    }

    // -----------------------------------------------------------------------
    // Request records
    // -----------------------------------------------------------------------

    public record ProjectRequest(
            @NotBlank @Size(max = 200) String name,
            String description,
            @NotNull UUID ownerOrganizationId,
            UUID clientId) {}

    public record InvitationRequest(
            @NotNull UUID organizationId,
            @NotNull UUID recipientUserId) {}
}
