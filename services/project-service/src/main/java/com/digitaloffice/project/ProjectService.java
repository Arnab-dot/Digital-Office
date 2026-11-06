package com.digitaloffice.project;

import com.digitaloffice.contracts.MembershipSummary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for project operations with organization-membership-aware authorization.
 *
 * <p>Authorization pattern:
 * <ol>
 *   <li>Look up the actor's ACTIVE org memberships via {@link OrganizationMembershipClient}.
 *   <li>For per-project access checks, verify that any of those orgs holds an ACTIVE
 *       participation in the project, OR that the actor is the project creator.
 * </ol>
 *
 * <p>Fail-closed: if Organization_Service is unreachable, a
 * {@link ServiceUnavailableException} (HTTP 503) propagates to the caller.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ParticipationRepository participationRepository;
    private final OrganizationMembershipClient membershipClient;

    public ProjectService(ProjectRepository projectRepository,
                          ParticipationRepository participationRepository,
                          OrganizationMembershipClient membershipClient) {
        this.projectRepository = projectRepository;
        this.participationRepository = participationRepository;
        this.membershipClient = membershipClient;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Creates a new project.
     *
     * <p>Validates that {@code ownerOrganizationId} is an organization where the actor
     * holds an ACTIVE membership before creating the project.
     *
     * @throws IllegalArgumentException    (HTTP 422) if actor is not a member of the org
     * @throws ServiceUnavailableException (HTTP 503) if Organization_Service is unreachable
     */
    @Transactional
    public Project create(String name, String description, UUID ownerOrganizationId,
                          UUID clientId, UUID actor, String bearerToken) {
        // Requirement 1.2: validate ownerOrganizationId is an org where actor has ACTIVE membership
        if (!membershipClient.hasActiveMembership(actor, ownerOrganizationId, bearerToken)) {
            throw new IllegalArgumentException(
                    "Actor does not have an active membership in organization " + ownerOrganizationId);
        }
        Project project = projectRepository.save(
                new Project(name, description, ownerOrganizationId, clientId, actor));
        participationRepository.save(
                new ProjectParticipation(project.getId(), ownerOrganizationId, ProjectRole.PROJECT_OWNER, actor));
        return project;
    }

    /**
     * Returns all projects accessible to the actor: projects where the actor's org has
     * ACTIVE participation, or the actor is the project creator.
     *
     * <p>Requirement 1.8: replace the old {@code createdBy} filter.
     */
    public List<Project> list(UUID actor, String bearerToken) {
        Set<UUID> actorOrgIds = resolveActorOrgIds(actor, bearerToken);

        if (actorOrgIds.isEmpty()) {
            // Actor has no memberships — return only projects they personally created
            return projectRepository.findByOwnerOrganizationIdInOrCreatedBy(Set.of(), actor);
        }

        // Find project IDs from participation records where any of actor's orgs participates
        Set<UUID> participatingProjectIds = participationRepository
                .findByOrganizationIdInAndStatus(actorOrgIds, ParticipationStatus.ACTIVE)
                .stream()
                .map(ProjectParticipation::getProjectId)
                .collect(Collectors.toSet());

        // Load all projects the actor can access: those they created + those in participatingProjectIds
        return projectRepository.findAll().stream()
                .filter(p -> participatingProjectIds.contains(p.getId()) || p.getCreatedBy().equals(actor))
                .toList();
    }

    /**
     * Returns a single project, verifying actor has access.
     *
     * @throws NoSuchElementException  (HTTP 404) if project not found
     * @throws AccessDeniedException   (HTTP 403) if actor has no access
     */
    public Project get(UUID projectId, UUID actor, String bearerToken) {
        Project project = findProject(projectId);
        requireProjectAccess(project, actor, bearerToken);
        return project;
    }

    /**
     * Updates a project's name and description.
     *
     * @throws NoSuchElementException  (HTTP 404) if project not found
     * @throws AccessDeniedException   (HTTP 403) if actor has no access
     * @throws IllegalStateException   (HTTP 409) if project is closed
     */
    @Transactional
    public Project update(UUID projectId, String name, String description,
                          UUID actor, String bearerToken) {
        Project project = findProject(projectId);
        requireProjectAccess(project, actor, bearerToken);
        project.update(name, description);
        return projectRepository.save(project);
    }

    /**
     * Closes a project.
     *
     * @throws NoSuchElementException  (HTTP 404) if project not found
     * @throws AccessDeniedException   (HTTP 403) if actor has no access
     * @throws IllegalStateException   (HTTP 409) if project is already closed
     */
    @Transactional
    public void close(UUID projectId, UUID actor, String bearerToken) {
        Project project = findProject(projectId);
        requireProjectAccess(project, actor, bearerToken);
        project.close();
        projectRepository.save(project);
    }

    /**
     * Returns the list of participants for a project.
     *
     * @throws NoSuchElementException  (HTTP 404) if project not found
     * @throws AccessDeniedException   (HTTP 403) if actor has no access
     */
    public List<ProjectParticipation> participants(UUID projectId, UUID actor, String bearerToken) {
        Project project = findProject(projectId);
        requireProjectAccess(project, actor, bearerToken);
        return participationRepository.findByProjectId(projectId);
    }

    // -----------------------------------------------------------------------
    // Authorization helpers
    // -----------------------------------------------------------------------

    /**
     * Verifies that the actor either created the project or belongs to an organization
     * with ACTIVE participation in it.
     *
     * <p>Uses {@link OrganizationMembershipClient} to resolve the actor's org IDs,
     * then checks the participation records against those org IDs.
     *
     * @throws AccessDeniedException       (HTTP 403) if actor has no access
     * @throws ServiceUnavailableException (HTTP 503) if Organization_Service is unreachable
     */
    void requireProjectAccess(Project project, UUID actor, String bearerToken) {
        // Creator always has access
        if (project.getCreatedBy().equals(actor)) {
            return;
        }

        // Resolve actor's ACTIVE org memberships
        Set<UUID> actorOrgIds = resolveActorOrgIds(actor, bearerToken);
        if (actorOrgIds.isEmpty()) {
            throw new AccessDeniedException("Actor has no active organization memberships");
        }

        // Check if any of actor's orgs has ACTIVE participation in the project
        boolean hasParticipation = actorOrgIds.stream().anyMatch(orgId ->
                participationRepository.findByProjectIdAndOrganizationIdAndStatus(
                        project.getId(), orgId, ParticipationStatus.ACTIVE).isPresent());

        if (!hasParticipation) {
            throw new AccessDeniedException("Actor's organization does not participate in this project");
        }
    }

    /**
     * Resolves the set of organization IDs where the actor holds an ACTIVE membership.
     *
     * @throws ServiceUnavailableException (HTTP 503) if Organization_Service is unreachable
     */
    private Set<UUID> resolveActorOrgIds(UUID actor, String bearerToken) {
        List<MembershipSummary> memberships = membershipClient.getActiveMemberships(actor, bearerToken);
        if (memberships == null) {
            return Set.of();
        }
        return memberships.stream()
                .map(MembershipSummary::organizationId)
                .collect(Collectors.toSet());
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));
    }
}
