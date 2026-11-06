package com.digitaloffice.work;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "milestones")
public class Milestone {
    @Id private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(name = "workstream_id") private UUID workstreamId;
    @Column(nullable = false) private String title;
    private String description;
    private LocalDate dueDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MilestoneStatus status;
    @Column(nullable = false) private UUID createdBy;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Milestone() {}
    public Milestone(UUID projectId, UUID workstreamId, String title, String description, LocalDate dueDate, UUID createdBy) {
        this.id = UUID.randomUUID(); this.projectId = projectId; this.workstreamId = workstreamId;
        this.title = title; this.description = description; this.dueDate = dueDate; this.createdBy = createdBy;
        this.status = MilestoneStatus.TODO; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UUID getWorkstreamId() { return workstreamId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public MilestoneStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public void update(String title, String description, LocalDate dueDate) { ensureMutable(); this.title = title; this.description = description; this.dueDate = dueDate; updatedAt = Instant.now(); }
    public void transition(MilestoneStatus next) {
        if (status == MilestoneStatus.TODO && next != MilestoneStatus.IN_PROGRESS && next != MilestoneStatus.CANCELLED
                || status == MilestoneStatus.IN_PROGRESS && next != MilestoneStatus.DONE && next != MilestoneStatus.TODO
                || status == MilestoneStatus.DONE && next != MilestoneStatus.IN_PROGRESS
                || status == MilestoneStatus.CANCELLED && next != MilestoneStatus.TODO) {
            throw new IllegalArgumentException("Invalid milestone transition");
        }
        status = next; updatedAt = Instant.now();
    }
    private void ensureMutable() { if (status == MilestoneStatus.DONE || status == MilestoneStatus.CANCELLED) throw new IllegalStateException("Completed milestone cannot be edited"); }
}
