package com.digitaloffice.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipationRepository extends JpaRepository<ProjectParticipation, UUID> {

    List<ProjectParticipation> findByProjectId(UUID id);

    Optional<ProjectParticipation> findByProjectIdAndOrganizationId(UUID projectId, UUID organizationId);

    List<ProjectParticipation> findByProjectIdAndStatus(UUID projectId, ParticipationStatus status);

    /**
     * Returns the participation record for a specific (project, organization, status) triple.
     * Used to check whether an actor's org has ACTIVE participation in a project.
     */
    Optional<ProjectParticipation> findByProjectIdAndOrganizationIdAndStatus(
            UUID projectId, UUID organizationId, ParticipationStatus status);

    /**
     * Returns all ACTIVE participations for any of the given organization IDs.
     * Used to find all projects that any of the actor's orgs participate in.
     */
    List<ProjectParticipation> findByOrganizationIdInAndStatus(
            Collection<UUID> organizationIds, ParticipationStatus status);
}
