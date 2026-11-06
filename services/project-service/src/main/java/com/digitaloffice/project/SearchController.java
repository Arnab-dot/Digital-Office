package com.digitaloffice.project;

import com.digitaloffice.contracts.MembershipSummary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final ProjectRepository projects;
    private final OrganizationMembershipClient memberships;
    private final ParticipationRepository participations;

    public SearchController(ProjectRepository projects, OrganizationMembershipClient memberships,
                            ParticipationRepository participations) {
        this.projects = projects;
        this.memberships = memberships;
        this.participations = participations;
    }

    @GetMapping("/projects")
    public List<Project> search(@RequestParam String keyword, @AuthenticationPrincipal Jwt jwt,
                                @RequestHeader("Authorization") String token) {
        String term = keyword == null ? "" : keyword.trim();
        if (term.isEmpty() || term.length() > 200)
            throw new IllegalArgumentException("keyword must be 1-200 characters");
        UUID actor = UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
        List<UUID> orgIds = memberships.getActiveMemberships(actor, token).stream()
                .map(MembershipSummary::organizationId).toList();
        Set<UUID> projectIds = orgIds.isEmpty() ? Set.of() : participations
                .findByOrganizationIdInAndStatus(orgIds, ParticipationStatus.ACTIVE).stream()
                .map(ProjectParticipation::getProjectId).collect(Collectors.toSet());
        // Avoid an empty IN clause while still allowing the actor to find projects they created.
        if (projectIds.isEmpty()) {
            return projects.findAll().stream()
                    .filter(p -> p.getCreatedBy().equals(actor) && p.getName().toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return projects.searchAccessible(term, projectIds, actor);
    }
}
