package com.digitaloffice.work;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workstreams")
public class Workstream {
    @Id private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(nullable = false) private String name;
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WorkstreamStatus status;
    @Column(nullable = false) private UUID createdBy;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Workstream() {}
    public Workstream(UUID projectId, String name, String description, UUID createdBy) {
        this.id = UUID.randomUUID(); this.projectId = projectId; this.name = name;
        this.description = description; this.createdBy = createdBy;
        this.status = WorkstreamStatus.ACTIVE; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public WorkstreamStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public void update(String name, String description) { ensureActive(); this.name = name; this.description = description; updatedAt = Instant.now(); }
    public void archive() { ensureActive(); status = WorkstreamStatus.ARCHIVED; updatedAt = Instant.now(); }
    private void ensureActive() { if (status == WorkstreamStatus.ARCHIVED) throw new IllegalStateException("Archived workstream cannot be changed"); }
}
