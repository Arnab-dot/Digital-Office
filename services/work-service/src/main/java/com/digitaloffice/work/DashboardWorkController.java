package com.digitaloffice.work;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardWorkController {
    private final TaskRepository tasks;
    public DashboardWorkController(TaskRepository tasks) { this.tasks = tasks; }

    @GetMapping("/active-task-count")
    public long activeTaskCount(@AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
        return tasks.countByAssigneeIdAndStatusIn(actor, List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS));
    }
}
