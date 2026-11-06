package com.digitaloffice.project;

import org.springframework.security.access.AccessDeniedException;
import com.digitaloffice.contracts.DomainEventPublisher;
import com.digitaloffice.contracts.UserNotificationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class ProjectInvitationService {
    private final ProjectRepository projects;
    private final ParticipationRepository participations;
    private final ProjectInvitationRepository invitations;
    private final DomainEventPublisher domainEvents;

    public ProjectInvitationService(ProjectRepository projects, ParticipationRepository participations,
                                    ProjectInvitationRepository invitations, DomainEventPublisher domainEvents) {
        this.projects = projects;
        this.participations = participations;
        this.invitations = invitations;
        this.domainEvents = domainEvents;
    }

    @Transactional
    public ProjectInvitation invite(UUID projectId, UUID organizationId, UUID recipientUserId, UUID actor) {
        Project project = ownerProject(projectId, actor);
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new IllegalStateException("Closed project cannot receive invitations");
        }
        if (participations.findByProjectIdAndOrganizationId(projectId, organizationId)
                .filter(p -> p.getStatus() == ParticipationStatus.ACTIVE).isPresent()) {
            throw new IllegalStateException("Organization already participates in this project");
        }
        if (invitations.findByProjectIdAndRecipientOrganizationIdAndRecipientUserIdAndStatus(
                projectId, organizationId, recipientUserId, ProjectInvitationStatus.PENDING).isPresent()) {
            throw new IllegalStateException("A pending invitation already exists");
        }
        ProjectInvitation invitation = invitations.save(new ProjectInvitation(projectId, organizationId, recipientUserId, actor,
                Instant.now().plusSeconds(7 * 86400)));
        domainEvents.publish(new UserNotificationEvent("PROJECT_INVITATION", projectId, actor,
                Instant.now(), recipientUserId, "PROJECT_INVITATION", "{\"projectId\":\"" + projectId + "\"}"));
        return invitation;
    }

    @Transactional
    public ProjectParticipation resolve(UUID invitationId, UUID actor, boolean accepted) {
        ProjectInvitation invitation = invitations.findById(invitationId).orElseThrow();
        if (!invitation.getRecipientUserId().equals(actor)) {
            throw new AccessDeniedException("Invitation recipient required");
        }
        if (invitation.expired()) {
            invitation.expire();
            invitations.save(invitation);
            throw new IllegalStateException("Invitation expired");
        }
        if (!accepted) {
            invitation.reject();
            invitations.save(invitation);
            return null;
        }
        invitation.accept();
        invitations.save(invitation);
        return participations.findByProjectIdAndOrganizationId(
                        invitation.getProjectId(), invitation.getRecipientOrganizationId())
                .orElseGet(() -> participations.save(new ProjectParticipation(
                        invitation.getProjectId(), invitation.getRecipientOrganizationId(),
                        ProjectRole.COLLABORATOR, actor)));
    }

    @Transactional
    public void revoke(UUID invitationId, UUID actor) {
        ProjectInvitation invitation = invitations.findById(invitationId).orElseThrow();
        ownerProject(invitation.getProjectId(), actor);
        invitation.revoke();
        invitations.save(invitation);
    }

    public List<ProjectInvitation> forProject(UUID projectId, UUID actor) {
        ownerProject(projectId, actor);
        return invitations.findByProjectId(projectId);
    }

    public List<ProjectInvitation> pendingFor(UUID actor) {
        return invitations.findByRecipientUserIdAndStatus(actor, ProjectInvitationStatus.PENDING);
    }

    private Project ownerProject(UUID id, UUID actor) {
        Project project = projects.findById(id).orElseThrow();
        if (!project.getCreatedBy().equals(actor)) {
            throw new AccessDeniedException("Project owner permission required");
        }
        return project;
    }
}
