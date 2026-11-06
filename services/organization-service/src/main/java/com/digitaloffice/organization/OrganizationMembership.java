package com.digitaloffice.organization;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="organization_memberships", uniqueConstraints=@UniqueConstraint(name="uk_org_user", columnNames={"organization_id","user_id"}))
public class OrganizationMembership {
    @Id private UUID id;
    @Column(name="organization_id",nullable=false) private UUID organizationId;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private OrganizationRole role;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private MembershipStatus status;
    @Column(nullable=false,updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected OrganizationMembership(){}
    public OrganizationMembership(UUID organizationId,UUID userId,OrganizationRole role){
        this.id=UUID.randomUUID();this.organizationId=organizationId;this.userId=userId;this.role=role;
        this.status=MembershipStatus.ACTIVE;this.createdAt=Instant.now();this.updatedAt=createdAt;
    }
    public UUID getId(){return id;} public UUID getOrganizationId(){return organizationId;} public UUID getUserId(){return userId;}
    public OrganizationRole getRole(){return role;} public MembershipStatus getStatus(){return status;}
    public void changeRole(OrganizationRole role){this.role=role;this.updatedAt=Instant.now();}
    public void changeStatus(MembershipStatus status){this.status=status;this.updatedAt=Instant.now();}
}
