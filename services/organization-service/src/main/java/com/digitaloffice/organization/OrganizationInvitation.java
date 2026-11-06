package com.digitaloffice.organization;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="organization_invitations", indexes=@Index(name="ix_invite_email_status", columnList="invitee_email,status"))
public class OrganizationInvitation {
    @Id private UUID id;
    @Column(name="organization_id",nullable=false) private UUID organizationId;
    @Column(name="invitee_email",nullable=false) private String inviteeEmail;
    @Column(nullable=false) private UUID inviterId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private OrganizationRole intendedRole;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private InvitationStatus status;
    @Column(nullable=false) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    private Instant decidedAt;
    protected OrganizationInvitation(){}
    public OrganizationInvitation(UUID organizationId,String email,UUID inviterId,OrganizationRole role,String tokenHash,Instant expiresAt){
        this.id=UUID.randomUUID();this.organizationId=organizationId;this.inviteeEmail=email;this.inviterId=inviterId;
        this.intendedRole=role;this.tokenHash=tokenHash;this.expiresAt=expiresAt;this.status=InvitationStatus.PENDING;
    }
    public UUID getId(){return id;} public UUID getOrganizationId(){return organizationId;} public String getInviteeEmail(){return inviteeEmail;}
    public OrganizationRole getIntendedRole(){return intendedRole;} public InvitationStatus getStatus(){return status;}
    public Instant getExpiresAt(){return expiresAt;} public String getTokenHash(){return tokenHash;}
    public void transition(InvitationStatus next){if(status!=InvitationStatus.PENDING)throw new IllegalStateException("Invitation is not pending");status=next;decidedAt=Instant.now();}
    public boolean expired(){return expiresAt.isBefore(Instant.now());}
}
