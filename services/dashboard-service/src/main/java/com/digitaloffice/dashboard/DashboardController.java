package com.digitaloffice.dashboard;
import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*; import java.nio.charset.StandardCharsets; import java.util.UUID;
@RestController @RequestMapping("/api/v1/dashboard") public class DashboardController {
 private final DashboardService service; public DashboardController(DashboardService s){service=s;}
 @GetMapping public DashboardService.DashboardSummary get(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Authorization") String token){return service.getSummary(UUID.nameUUIDFromBytes(("oidc:"+jwt.getSubject()).getBytes(StandardCharsets.UTF_8)),token);}
}
