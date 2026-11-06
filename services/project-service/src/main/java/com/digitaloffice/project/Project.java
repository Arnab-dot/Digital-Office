package com.digitaloffice.project;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="projects",indexes=@Index(name="ix_projects_owner_status",columnList="owner_organization_id,status"))
public class Project {
 @Id private UUID id; @Column(nullable=false) private String name; private String description; @Column(name="owner_organization_id",nullable=false) private UUID ownerOrganizationId; private UUID clientId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ProjectStatus status; @Column(nullable=false) private UUID createdBy; @Column(nullable=false) private Instant createdAt; @Column(nullable=false) private Instant updatedAt;
 protected Project(){} public Project(String n,String d,UUID org,UUID client,UUID actor){id=UUID.randomUUID();name=n;description=d;ownerOrganizationId=org;clientId=client;createdBy=actor;status=ProjectStatus.ACTIVE;createdAt=Instant.now();updatedAt=createdAt;}
 public UUID getId(){return id;} public String getName(){return name;} public String getDescription(){return description;} public UUID getOwnerOrganizationId(){return ownerOrganizationId;} public UUID getClientId(){return clientId;} public UUID getCreatedBy(){return createdBy;} public ProjectStatus getStatus(){return status;} public void update(String n,String d){if(status==ProjectStatus.CLOSED)throw new IllegalStateException("Closed project cannot be edited");name=n;description=d;updatedAt=Instant.now();} public void close(){if(status==ProjectStatus.CLOSED)throw new IllegalStateException("Project is already closed");status=ProjectStatus.CLOSED;updatedAt=Instant.now();}
}
