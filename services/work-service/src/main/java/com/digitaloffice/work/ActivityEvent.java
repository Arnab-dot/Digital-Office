package com.digitaloffice.work;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_events")
public class ActivityEvent {
    @Id private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(name = "actor_id", nullable = false) private UUID actorId;
    @Enumerated(EnumType.STRING) @Column(name = "action_type", nullable = false) private WorkActivityActionType actionType;
    @Column(name = "subject_id", nullable = false) private UUID subjectId;
    @Column(name = "subject_type", nullable = false) private String subjectType;
    private String metadata;
    @Column(name = "event_at", nullable = false) private Instant eventAt;

    protected ActivityEvent() {}
    public ActivityEvent(UUID projectId, UUID actorId, WorkActivityActionType actionType,
                         UUID subjectId, String subjectType, String metadata) {
        this.id = UUID.randomUUID(); this.projectId = projectId; this.actorId = actorId;
        this.actionType = actionType; this.subjectId = subjectId; this.subjectType = subjectType;
        this.metadata = metadata; this.eventAt = Instant.now();
    }
    public UUID getId() { return id; } public UUID getProjectId() { return projectId; }
    public UUID getActorId() { return actorId; } public WorkActivityActionType getActionType() { return actionType; }
    public UUID getSubjectId() { return subjectId; } public String getSubjectType() { return subjectType; }
    public String getMetadata() { return metadata; } public Instant getEventAt() { return eventAt; }
}
