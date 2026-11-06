package com.digitaloffice.work;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_comments")
public class TaskComment {
    @Id private UUID id;
    @Column(name = "task_id", nullable = false) private UUID taskId;
    @Column(name = "author_id", nullable = false, updatable = false) private UUID authorId;
    @Column(nullable = false) private String content;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected TaskComment() {}
    public TaskComment(UUID taskId, UUID authorId, String content) {
        this.id = UUID.randomUUID();
        this.taskId = taskId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getTaskId() { return taskId; }
    public UUID getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
