package com.digitaloffice.organization;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="organizations", uniqueConstraints=@UniqueConstraint(name="uk_org_slug", columnNames="slug"))
public class Organization {
    @Id private UUID id;
    @Column(nullable=false) private String name;
    @Column(nullable=false) private String slug;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private OrganizationStatus status;
    @Column(nullable=false, updatable=false) private UUID ownerId;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected Organization() {}
    public Organization(String name, String slug, UUID ownerId) {
        this.id=UUID.randomUUID(); this.name=name; this.slug=slug; this.ownerId=ownerId;
        this.status=OrganizationStatus.ACTIVE; this.createdAt=Instant.now(); this.updatedAt=createdAt;
    }
    public UUID getId(){return id;} public String getName(){return name;} public String getSlug(){return slug;}
    public OrganizationStatus getStatus(){return status;} public UUID getOwnerId(){return ownerId;}
    public void update(String name,String slug){this.name=name;this.slug=slug;this.updatedAt=Instant.now();}
    public void setStatus(OrganizationStatus status){this.status=status;this.updatedAt=Instant.now();}
}
