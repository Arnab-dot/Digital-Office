package com.digitaloffice.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {
    List<ProjectInvitation> findByProjectId(UUID projectId);
    List<ProjectInvitation> findByRecipientUserIdAndStatus(UUID userId, ProjectInvitationStatus status);
    Optional<ProjectInvitation> findByProjectIdAndRecipientOrganizationIdAndRecipientUserIdAndStatus(
            UUID projectId, UUID organizationId, UUID userId, ProjectInvitationStatus status);
}
