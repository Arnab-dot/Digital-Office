package com.digitaloffice.work;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {
    @Mock TaskRepository tasks;
    @Mock TaskDependencyRepository dependencies;
    private TaskDependencyService service;
    private final UUID actor = UUID.randomUUID();
    private final UUID project = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TaskDependencyService(tasks, dependencies);
    }

    @Test
    void acceptsAcyclicDependency() {
        Task first = task();
        Task second = task();
        when(tasks.findById(first.getId())).thenReturn(Optional.of(first));
        when(tasks.findById(second.getId())).thenReturn(Optional.of(second));
        when(tasks.findByProjectId(project)).thenReturn(List.of(first, second));
        when(dependencies.findByTaskIdIn(any())).thenReturn(List.of());
        when(dependencies.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDependency result = service.add(first.getId(), second.getId(), actor);

        assertEquals(first.getId(), result.getTaskId());
        assertEquals(second.getId(), result.getDependsOnTaskId());
    }

    @Test
    void rejectsSelfDependency() {
        Task task = task();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.add(task.getId(), task.getId(), actor));

        assertEquals("A task cannot depend on itself", error.getMessage());
        verifyNoInteractions(dependencies);
    }

    @Test
    void rejectsDependencyAcrossProjects() {
        Task first = task();
        Task other = new Task(UUID.randomUUID(), "Other", null, "HIGH", null, actor);
        when(tasks.findById(first.getId())).thenReturn(Optional.of(first));
        when(tasks.findById(other.getId())).thenReturn(Optional.of(other));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.add(first.getId(), other.getId(), actor));

        assertEquals("Dependencies must belong to the same project", error.getMessage());
    }

    @Test
    void rejectsLongCycle() {
        Task a = task();
        Task b = task();
        Task c = task();
        when(tasks.findById(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Stream.of(a, b, c).filter(task -> task.getId().equals(id)).findFirst();
        });
        when(tasks.findByProjectId(project)).thenReturn(List.of(a, b, c));
        when(dependencies.findByTaskIdIn(any())).thenReturn(List.of(
                new TaskDependency(b.getId(), a.getId(), actor),
                new TaskDependency(c.getId(), b.getId(), actor)));
        when(dependencies.existsByTaskIdAndDependsOnTaskId(a.getId(), c.getId())).thenReturn(false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.add(a.getId(), c.getId(), actor));

        assertEquals("Task dependency would create a cycle", error.getMessage());
        verify(dependencies, never()).save(any());
    }

    @Test
    void rejectsDuplicateDependency() {
        Task first = task();
        Task second = task();
        when(tasks.findById(first.getId())).thenReturn(Optional.of(first));
        when(tasks.findById(second.getId())).thenReturn(Optional.of(second));
        when(dependencies.existsByTaskIdAndDependsOnTaskId(first.getId(), second.getId())).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.add(first.getId(), second.getId(), actor));

        assertEquals("Task dependency already exists", error.getMessage());
        verify(dependencies, never()).save(any());
    }

    private Task task() {
        return new Task(project, "Task", null, "MEDIUM", null, actor);
    }
}
