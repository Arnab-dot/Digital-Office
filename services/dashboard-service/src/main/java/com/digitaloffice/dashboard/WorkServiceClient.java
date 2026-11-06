package com.digitaloffice.dashboard;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.*;

@Component
public class WorkServiceClient {
    private final RestClient work;
    private final RestClient projects;

    public WorkServiceClient(@Value("${internal.work-service.url:http://localhost:8085}") String workUrl,
                             @Value("${internal.project-service.url:http://localhost:8084}") String projectUrl) {
        this.work = RestClient.builder().baseUrl(workUrl).build();
        this.projects = RestClient.builder().baseUrl(projectUrl).build();
    }

    public int countActiveTasks(UUID actorId, String bearerToken) {
        Integer count = work.get().uri("/api/v1/dashboard/active-task-count")
                .header("Authorization", bearerToken).retrieve().body(Integer.class);
        return count == null ? 0 : count;
    }

    public List<DashboardService.MilestoneSummary> getUpcomingMilestones(UUID actorId, String bearerToken) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(14);
        List<DashboardService.MilestoneSummary> result = new ArrayList<>();
        for (UUID projectId : accessibleProjectIds(bearerToken)) {
            List<Map<String, Object>> milestones = work.get().uri("/api/v1/projects/{id}/milestones", projectId)
                    .header("Authorization", bearerToken).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (milestones == null) continue;
            for (Map<String, Object> m : milestones) {
                Object due = m.get("dueDate");
                if (due == null) continue;
                LocalDate date = LocalDate.parse(due.toString());
                if (!date.isBefore(today) && !date.isAfter(end))
                    result.add(new DashboardService.MilestoneSummary(UUID.fromString(m.get("id").toString()),
                            Objects.toString(m.get("title"), ""), date));
            }
        }
        result.sort(Comparator.comparing(DashboardService.MilestoneSummary::dueDate));
        return result;
    }

    private List<UUID> accessibleProjectIds(String token) {
        List<Map<String, Object>> rows = projects.get().uri("/api/v1/projects")
                .header("Authorization", token).retrieve().body(new ParameterizedTypeReference<>() {});
        if (rows == null) return List.of();
        return rows.stream().map(p -> UUID.fromString(p.get("id").toString())).distinct().toList();
    }
}
