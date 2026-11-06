package com.digitaloffice.document;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="document_activity_events")
public class DocumentActivityEvent {
 @Id private UUID id; @Column(nullable=false) private UUID projectId; @Column(nullable=false) private UUID actorId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private DocumentActivityActionType actionType;
 @Column(nullable=false) private UUID subjectId; @Column(nullable=false) private String subjectType; private String metadata;
 @Column(nullable=false) private Instant eventAt; protected DocumentActivityEvent(){}
 public DocumentActivityEvent(UUID p,UUID a,DocumentActivityActionType t,UUID s,String st,String m){id=UUID.randomUUID();projectId=p;actorId=a;actionType=t;subjectId=s;subjectType=st;metadata=m;eventAt=Instant.now();}
 public UUID getId(){return id;} public UUID getProjectId(){return projectId;} public UUID getActorId(){return actorId;} public DocumentActivityActionType getActionType(){return actionType;} public UUID getSubjectId(){return subjectId;} public String getSubjectType(){return subjectType;} public String getMetadata(){return metadata;} public Instant getEventAt(){return eventAt;}
}
