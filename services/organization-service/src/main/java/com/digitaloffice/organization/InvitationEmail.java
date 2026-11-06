package com.digitaloffice.organization;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitation_emails")
public class InvitationEmail {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(name = "invitation_token", nullable = false)
    private String invitationToken;

    @Column(nullable = false)
    private String status;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected InvitationEmail() {}

    public InvitationEmail(UUID organizationId, String recipientEmail, String subject, String invitationToken, String status) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.invitationToken = invitationToken;
        this.status = status;
        this.sentAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getSubject() { return subject; }
    public String getInvitationToken() { return invitationToken; }
    public String getStatus() { return status; }
    public Instant getSentAt() { return sentAt; }
}

