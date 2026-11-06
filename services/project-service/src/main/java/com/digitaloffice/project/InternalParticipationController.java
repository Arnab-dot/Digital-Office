package com.digitaloffice.project;

import com.digitaloffice.contracts.ParticipationSummary;
import com.digitaloffice.contracts.ProjectParticipationRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Internal endpoint used by other services (work-service, document-service) to look up
 * active project participations without a direct DB dependency.
 *
 * <p>Secured by the JWT resource server config — callers must present a valid JWT.
 * This endpoint is NOT exposed through the API Gateway.
 */
@RestController
@RequestMapping("/internal")
public class InternalParticipationController {

    private final ParticipationRepository participationRepository;
    private final ProjectRepository projectRepository;

    public InternalParticipationController(ParticipationRepository participationRepository,
                                           ProjectRepository projectRepository) {
        this.participationRepository = participationRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Returns all ACTIVE participations for the given project.
     *
     * @param projectId the project whose active participations are requested
     * @return list of {@link ParticipationSummary} records containing organizationId and role
     */
    @GetMapping("/participations")
    public List<ParticipationSummary> getActiveParticipations(@RequestParam UUID projectId) {
        return participationRepository
                .findByProjectIdAndStatus(projectId, ParticipationStatus.ACTIVE)
                .stream()
                .map(p -> new ParticipationSummary(p.getOrganizationId(), toContractRole(p.getRole())))
                .toList();
    }

    @GetMapping("/participations/projects")
    public List<UUID> getActiveProjectIds(@RequestParam List<UUID> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) return List.of();
        return participationRepository.findByOrganizationIdInAndStatus(organizationIds, ParticipationStatus.ACTIVE)
                .stream().map(ProjectParticipation::getProjectId).distinct().toList();
    }

    @GetMapping("/projects/{projectId}/status")
    public ProjectStatusResponse projectStatus(@org.springframework.web.bind.annotation.PathVariable UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Project not found"));
        return new ProjectStatusResponse(project.getStatus().name());
    }

    public record ProjectStatusResponse(String status) {}

    /**
     * Maps the project-service's internal {@link ProjectRole} enum to the
     * platform-contracts {@link ProjectParticipationRole} enum so that downstream
     * services do not need a compile-time dependency on project-service.
     */
    private static ProjectParticipationRole toContractRole(ProjectRole role) {
        return switch (role) {
            case PROJECT_OWNER -> ProjectParticipationRole.PROJECT_OWNER;
            case PROJECT_ADMIN -> ProjectParticipationRole.PROJECT_ADMIN;
            case COLLABORATOR  -> ProjectParticipationRole.COLLABORATOR;
            case VIEWER        -> ProjectParticipationRole.VIEWER;
        };
    }
}
