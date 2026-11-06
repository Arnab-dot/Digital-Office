package com.digitaloffice.work;
import com.digitaloffice.contracts.MembershipSummary; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*; import java.nio.charset.StandardCharsets; import java.util.*;
@RestController @RequestMapping("/api/v1/search") public class SearchController {
 private final TaskRepository tasks; private final OrganizationMembershipClient memberships; private final ProjectParticipationClient participations;
 public SearchController(TaskRepository t,OrganizationMembershipClient m,ProjectParticipationClient p){tasks=t;memberships=m;participations=p;}
 @GetMapping("/tasks") public List<Task> search(@RequestParam String keyword,@AuthenticationPrincipal Jwt jwt,@RequestHeader("Authorization") String token){if(keyword==null||keyword.trim().isEmpty()||keyword.trim().length()>200)throw new IllegalArgumentException("keyword must be 1-200 characters");UUID actor=UUID.nameUUIDFromBytes(("oidc:"+jwt.getSubject()).getBytes(StandardCharsets.UTF_8));List<UUID> orgIds=memberships.getActiveMemberships(actor,token).stream().map(MembershipSummary::organizationId).toList();List<UUID> projects=participations.getActiveProjectIds(orgIds,token);if(projects.isEmpty())return List.of();return tasks.searchByTitle(keyword.trim(),projects);}
}
