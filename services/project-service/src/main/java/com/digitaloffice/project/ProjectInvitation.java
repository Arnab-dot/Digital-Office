package com.digitaloffice.project;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_invitations")
public class ProjectInvitation {
    @Id
    private UUID id;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "recipient_organization_id", nullable = false)
    private UUID recipientOrganizationId;
    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;
    @Column(nullable = false)
    private UUID invitedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectInvitationStatus status;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant resolvedAt;

    protected ProjectInvitation() {}

    public ProjectInvitation(UUID projectId, UUID organizationId, UUID userId, UUID invitedBy, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.recipientOrganizationId = organizationId;
        this.recipientUserId = userId;
        this.invitedBy = invitedBy;
        this.status = ProjectInvitationStatus.PENDING;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UUID getRecipientOrganizationId() { return recipientOrganizationId; }
    public UUID getRecipientUserId() { return recipientUserId; }
    public UUID getInvitedBy() { return invitedBy; }
    public ProjectInvitationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public boolean expired() { return status == ProjectInvitationStatus.PENDING && expiresAt.isBefore(Instant.now()); }
    public void accept() { transition(ProjectInvitationStatus.ACCEPTED); }
    public void reject() { transition(ProjectInvitationStatus.REJECTED); }
    public void revoke() { transition(ProjectInvitationStatus.REVOKED); }
    public void expire() { transition(ProjectInvitationStatus.EXPIRED); }

    private void transition(ProjectInvitationStatus next) {
        if (status != ProjectInvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is already resolved");
        }
        status = next;
        resolvedAt = Instant.now();
    }
}
