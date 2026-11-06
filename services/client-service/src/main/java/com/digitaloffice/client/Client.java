package com.digitaloffice.client;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="clients",indexes=@Index(name="ix_clients_org_status",columnList="organization_id,status"))
public class Client {
 @Id private UUID id; @Column(name="organization_id",nullable=false) private UUID organizationId; @Column(nullable=false) private String name;
 private String description; private String contactEmail; @Enumerated(EnumType.STRING) @Column(nullable=false) private ClientStatus status;
 @Column(nullable=false,updatable=false) private UUID createdBy; @Column(nullable=false) private Instant createdAt; @Column(nullable=false) private Instant updatedAt;
 protected Client(){} public Client(UUID org,String name,String description,String contactEmail,UUID actor){id=UUID.randomUUID();organizationId=org;this.name=name;this.description=description;this.contactEmail=contactEmail;createdBy=actor;status=ClientStatus.ACTIVE;createdAt=Instant.now();updatedAt=createdAt;}
 public UUID getId(){return id;} public UUID getOrganizationId(){return organizationId;} public String getName(){return name;} public String getDescription(){return description;} public String getContactEmail(){return contactEmail;} public ClientStatus getStatus(){return status;}
 public UUID getCreatedBy(){return createdBy;}
 public void update(String n,String d,String e){name=n;description=d;contactEmail=e;updatedAt=Instant.now();} public void archive(){status=ClientStatus.ARCHIVED;updatedAt=Instant.now();}
}
