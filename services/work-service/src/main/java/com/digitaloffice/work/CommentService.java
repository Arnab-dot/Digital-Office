package com.digitaloffice.work;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class CommentService {
    private final TaskRepository tasks;
    private final CommentRepository comments;
    private final OrganizationMembershipClient memberships;
    private final ProjectParticipationClient participations;
    private final ActivityService activity;

    public CommentService(TaskRepository tasks, CommentRepository comments,
                          OrganizationMembershipClient memberships,
                          ProjectParticipationClient participations, ActivityService activity) {
        this.tasks = tasks; this.comments = comments; this.memberships = memberships;
        this.participations = participations; this.activity = activity;
    }
    @Transactional
    public TaskComment addComment(UUID taskId, String content, UUID actor, String token) {
        Task task = accessibleTask(taskId, actor, token);
        String value = content == null ? "" : content.trim();
        if (value.isEmpty() || value.length() > 10000) throw new IllegalArgumentException("Comment content must be 1-10000 characters");
        TaskComment saved = comments.save(new TaskComment(task.getId(), actor, value));
        activity.recordEvent(task.getProjectId(), actor, WorkActivityActionType.COMMENT_ADDED,
                saved.getId(), "TASK_COMMENT", null);
        return saved;
    }
    public List<TaskComment> getComments(UUID taskId, UUID actor, String token) {
        accessibleTask(taskId, actor, token);
        return comments.findByTaskIdOrderByCreatedAtAsc(taskId);
    }
    private Task accessibleTask(UUID id, UUID actor, String token) {
        Task task = tasks.findById(id).orElseThrow(() -> new NoSuchElementException("Task not found"));
        if (actor.equals(task.getAssigneeId())) return task;
        boolean allowed = memberships.getActiveMemberships(actor, token).stream()
                .anyMatch(m -> participations.hasActiveParticipation(task.getProjectId(), m.organizationId(), token));
        if (!allowed) throw new AccessDeniedException("Actor cannot access this task");
        return task;
    }
}
