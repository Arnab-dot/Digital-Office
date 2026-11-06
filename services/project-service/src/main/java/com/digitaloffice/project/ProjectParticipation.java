package com.digitaloffice.project;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="project_participations",uniqueConstraints=@UniqueConstraint(name="uk_project_org",columnNames={"project_id","organization_id"}))
public class ProjectParticipation {
 @Id private UUID id; @Column(name="project_id",nullable=false) private UUID projectId; @Column(name="organization_id",nullable=false) private UUID organizationId; @Enumerated(EnumType.STRING) @Column(nullable=false) private ProjectRole role; @Enumerated(EnumType.STRING) @Column(nullable=false) private ParticipationStatus status; @Column(nullable=false) private UUID addedBy; @Column(nullable=false) private Instant joinedAt;
 protected ProjectParticipation(){} public ProjectParticipation(UUID p,UUID o,ProjectRole r,UUID a){id=UUID.randomUUID();projectId=p;organizationId=o;role=r;addedBy=a;status=ParticipationStatus.ACTIVE;joinedAt=Instant.now();}
 public UUID getId(){return id;} public UUID getProjectId(){return projectId;} public UUID getOrganizationId(){return organizationId;} public ProjectRole getRole(){return role;} public ParticipationStatus getStatus(){return status;} public void setRole(ProjectRole r){role=r;} public void setStatus(ParticipationStatus s){status=s;}
}
