package com.digitaloffice.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for client management.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Extract the actor UUID from the JWT (via {@link #actor(Jwt)}).</li>
 *   <li>Extract the raw bearer token from the {@code Authorization} header and
 *       forward it to {@link ClientService} for downstream membership verification.</li>
 *   <li>Delegate all business logic and authorization checks to {@link ClientService}.</li>
 * </ul>
 *
 * <p>No authorization logic lives in this class — it is intentionally thin.
 */
@RestController
@RequestMapping("/api/v1")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    @PostMapping("/organizations/{orgId}/clients")
    public Client create(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ClientRequest request) {
        return clientService.create(orgId, actor(jwt), request, authorization);
    }

    @GetMapping("/organizations/{orgId}/clients")
    public List<Client> list(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Authorization") String authorization) {
        return clientService.list(orgId, actor(jwt), authorization);
    }

    @GetMapping("/clients/{id}")
    public Client get(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Authorization") String authorization) {
        return clientService.get(id, actor(jwt), authorization);
    }

    @PatchMapping("/clients/{id}")
    public Client update(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ClientRequest request) {
        return clientService.update(id, actor(jwt), request, authorization);
    }

    @PostMapping("/clients/{id}/archive")
    public void archive(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Authorization") String authorization) {
        clientService.archive(id, actor(jwt), authorization);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Derives the actor's internal UUID from the JWT {@code sub} claim.
     * Mirrors the mapping used across all services:
     * {@code UUID.nameUUIDFromBytes("oidc:" + subject)}.
     */
    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Request body record
    // -------------------------------------------------------------------------

    public record ClientRequest(
            @NotBlank @Size(max = 200) String name,
            String description,
            @Email String contactEmail) {
    }
}
