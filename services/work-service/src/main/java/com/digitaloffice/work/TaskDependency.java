package com.digitaloffice.work;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_dependencies",
        uniqueConstraints = @UniqueConstraint(name = "uk_task_dependency", columnNames = {"task_id", "depends_on_task_id"}))
public class TaskDependency {
    @Id
    private UUID id;
    @Column(name = "task_id", nullable = false)
    private UUID taskId;
    @Column(name = "depends_on_task_id", nullable = false)
    private UUID dependsOnTaskId;
    @Column(nullable = false)
    private UUID createdBy;
    @Column(nullable = false)
    private Instant createdAt;

    protected TaskDependency() {}

    public TaskDependency(UUID taskId, UUID dependsOnTaskId, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.taskId = taskId;
        this.dependsOnTaskId = dependsOnTaskId;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTaskId() { return taskId; }
    public UUID getDependsOnTaskId() { return dependsOnTaskId; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
