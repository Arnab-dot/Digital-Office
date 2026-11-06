package com.digitaloffice.dashboard;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {
    public record MilestoneSummary(UUID id, String title, LocalDate dueDate) {}
    public record ProjectInvitationSummary(UUID id, UUID projectId) {}
    public record DocumentVersionSummary(UUID id, UUID documentId) {}
    public record DashboardSummary(int activeTaskCount, List<MilestoneSummary> upcomingMilestones,
                                   List<ProjectInvitationSummary> pendingInvitations,
                                   List<DocumentVersionSummary> pendingReviews,
                                   int unreadNotificationCount, List<String> warnings) {}

    private final WorkServiceClient work;
    private final DocumentServiceClient documents;
    private final ProjectServiceClient projects;
    private final NotificationServiceClient notifications;

    public DashboardService(WorkServiceClient work, DocumentServiceClient documents,
                            ProjectServiceClient projects, NotificationServiceClient notifications) {
        this.work = work;
        this.documents = documents;
        this.projects = projects;
        this.notifications = notifications;
    }

    public DashboardSummary getSummary(UUID actor, String token) {
        List<String> warnings = new ArrayList<>();
        int activeTasks = 0;
        List<MilestoneSummary> milestones = List.of();
        List<ProjectInvitationSummary> invitations = List.of();
        List<DocumentVersionSummary> reviews = List.of();
        int unread = 0;
        try { activeTasks = work.countActiveTasks(actor, token); }
        catch (RuntimeException e) { warnings.add("Active task count is unavailable"); }
        try { milestones = work.getUpcomingMilestones(actor, token); }
        catch (RuntimeException e) { warnings.add("Upcoming milestones are unavailable"); }
        try { invitations = projects.getPendingInvitations(actor, token); }
        catch (RuntimeException e) { warnings.add("Pending project invitations are unavailable"); }
        try { reviews = documents.getPendingReviews(actor, token); }
        catch (RuntimeException e) { warnings.add("Pending document reviews are unavailable"); }
        try { unread = notifications.countUnreadNotifications(actor, token); }
        catch (RuntimeException e) { warnings.add("Unread notification count is unavailable"); }
        return new DashboardSummary(activeTasks, milestones, invitations, reviews, unread, warnings);
    }
}
