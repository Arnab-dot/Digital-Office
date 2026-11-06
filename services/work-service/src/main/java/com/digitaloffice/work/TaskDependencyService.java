package com.digitaloffice.work;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class TaskDependencyService {
    private final TaskRepository tasks;
    private final TaskDependencyRepository dependencies;

    public TaskDependencyService(TaskRepository tasks, TaskDependencyRepository dependencies) {
        this.tasks = tasks;
        this.dependencies = dependencies;
    }

    @Transactional
    public TaskDependency add(UUID taskId, UUID dependsOnTaskId, UUID actor) {
        if (taskId.equals(dependsOnTaskId)) {
            throw new IllegalArgumentException("A task cannot depend on itself");
        }
        Task task = task(taskId);
        Task dependency = task(dependsOnTaskId);
        requireAccess(task, actor);
        requireAccess(dependency, actor);
        if (!task.getProjectId().equals(dependency.getProjectId())) {
            throw new IllegalArgumentException("Dependencies must belong to the same project");
        }
        if (dependencies.existsByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId)) {
            throw new IllegalArgumentException("Task dependency already exists");
        }
        if (wouldCreateCycle(taskId, dependsOnTaskId, task.getProjectId())) {
            throw new IllegalArgumentException("Task dependency would create a cycle");
        }
        return dependencies.save(new TaskDependency(taskId, dependsOnTaskId, actor));
    }

    @Transactional
    public void remove(UUID taskId, UUID dependsOnTaskId, UUID actor) {
        Task task = task(taskId);
        Task dependency = task(dependsOnTaskId);
        requireAccess(task, actor);
        requireAccess(dependency, actor);
        dependencies.delete(dependencies.findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId).orElseThrow());
    }

    public List<TaskDependency> list(UUID taskId, UUID actor) {
        requireAccess(task(taskId), actor);
        return dependencies.findByTaskId(taskId);
    }

    private boolean wouldCreateCycle(UUID taskId, UUID dependsOnTaskId, UUID projectId) {
        Map<UUID, Set<UUID>> graph = new HashMap<>();
        for (TaskDependency edge : dependencies.findByTaskIdIn(
                tasks.findByProjectId(projectId).stream().map(Task::getId).toList())) {
            graph.computeIfAbsent(edge.getTaskId(), ignored -> new HashSet<>()).add(edge.getDependsOnTaskId());
        }
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> pending = new ArrayDeque<>();
        pending.push(dependsOnTaskId);
        while (!pending.isEmpty()) {
            UUID current = pending.pop();
            if (!visited.add(current)) continue;
            if (current.equals(taskId)) return true;
            graph.getOrDefault(current, Set.of()).forEach(pending::push);
        }
        return false;
    }

    private Task task(UUID id) { return tasks.findById(id).orElseThrow(); }

    private static void requireAccess(Task task, UUID actor) {
        if (!actor.equals(task.getCreatedBy()) && !actor.equals(task.getAssigneeId())) {
            throw new AccessDeniedException("Task access denied");
        }
    }
}
