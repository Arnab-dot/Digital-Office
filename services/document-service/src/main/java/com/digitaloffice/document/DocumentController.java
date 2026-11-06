package com.digitaloffice.document;

import com.digitaloffice.contracts.MembershipSummary;
import com.digitaloffice.contracts.ParticipationSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final OrganizationMembershipClient membershipClient;
    private final ProjectParticipationClient participationClient;
    private final DocumentActivityService activityService;
    private final ProjectStatusClient projectStatusClient;

    public DocumentController(DocumentRepository documents,
                              DocumentVersionRepository versions,
                              OrganizationMembershipClient membershipClient,
                              ProjectParticipationClient participationClient,
                              DocumentActivityService activityService,
                              ProjectStatusClient projectStatusClient) {
        this.documents = documents;
        this.versions = versions;
        this.membershipClient = membershipClient;
        this.participationClient = participationClient;
        this.activityService = activityService;
        this.projectStatusClient = projectStatusClient;
    }

    /** Derive the actor's internal UUID from the JWT sub claim. */
    private static UUID actorId(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes());
    }

    /** Extract the raw "Authorization: Bearer ..." header to forward to internal services. */
    private static String bearerToken(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    // -------------------------------------------------------------------------
    // CREATE  — actor's org must have ACTIVE participation in the project
    // -------------------------------------------------------------------------

    @PostMapping("/projects/{projectId}/documents")
    @Transactional
    public Document create(@PathVariable UUID projectId,
                           @AuthenticationPrincipal Jwt jwt,
                           @Valid @RequestBody DocumentRequest r,
                           HttpServletRequest request) {
        UUID actor = actorId(jwt);
        String token = bearerToken(request);

        requireParticipation(projectId, actor, token);
        projectStatusClient.requireOpen(projectId, token);

        Document d = documents.save(new Document(projectId, r.title(), r.description(), actor));
        DocumentVersion initial = versions.save(new DocumentVersion(d.getId(), 0, actor, "Initial version"));
        activityService.recordEvent(projectId, actor, DocumentActivityActionType.DOCUMENT_VERSION_CREATED,
                initial.getId(), "DOCUMENT_VERSION", null);
        return d;
    }

    // -------------------------------------------------------------------------
    // LIST  — participation-based: return all documents in projects where
    //         actor's org holds ACTIVE participation (Requirement 1.10)
    // -------------------------------------------------------------------------

    @GetMapping("/projects/{projectId}/documents")
    public List<Document> list(@PathVariable UUID projectId,
                               @AuthenticationPrincipal Jwt jwt,
                               HttpServletRequest request) {
        UUID actor = actorId(jwt);
        String token = bearerToken(request);

        requireParticipation(projectId, actor, token);

        // Return all ACTIVE documents in the project (not just those created by actor)
        return documents.findByProjectIdAndStatus(projectId, DocumentStatus.ACTIVE);
    }

    // -------------------------------------------------------------------------
    // GET  — participation-based read access (Requirement 1.4)
    // -------------------------------------------------------------------------

    @GetMapping("/documents/{id}")
    public Document get(@PathVariable UUID id,
                        @AuthenticationPrincipal Jwt jwt,
                        HttpServletRequest request) {
        UUID actor = actorId(jwt);
        String token = bearerToken(request);

        Document d = documents.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found"));

        requireParticipation(d.getProjectId(), actor, token);
        return d;
    }

    // -------------------------------------------------------------------------
    // UPDATE  — owner-only; also rejects archived document (→ 409)
    // -------------------------------------------------------------------------

    @PatchMapping("/documents/{id}")
    public Document update(@PathVariable UUID id,
                           @AuthenticationPrincipal Jwt jwt,
                           @Valid @RequestBody DocumentRequest r,
                           HttpServletRequest request) {
        UUID actor = actorId(jwt);
        String token = bearerToken(request);

        Document d = requireOwner(id, actor, token);
        projectStatusClient.requireOpen(d.getProjectId(), token);
        // Document.update() throws IllegalStateException (→ 409) when ARCHIVED
        d.update(r.title(), r.description());
        return documents.save(d);
    }

    // -------------------------------------------------------------------------
    // CREATE VERSION  — owner-only; rejects archived document (→ 409)
    // -------------------------------------------------------------------------

    @PostMapping("/documents/{id}/versions")
    @Transactional
    public Document version(@PathVariable UUID id,
                            @AuthenticationPrincipal Jwt jwt,
                            @RequestBody(required = false) VersionRequest r,
                            HttpServletRequest request) {
        UUID actor = actorId(jwt);
        String token = bearerToken(request);

        Document d = requireOwner(id, actor, token);
        projectStatusClient.requireOpen(d.getProjectId(), token);
        // Document.nextVersion() throws IllegalStateException (→ 409) when ARCHIVED
        d.nextVersion();
        Document saved = documents.save(d);
        DocumentVersion version = versions.save(new DocumentVersion(d.getId(), d.getCurrentVersion(), actor,
                r == null ? null : r.changeSummary()));
        activityService.recordEvent(d.getProjectId(), actor, DocumentActivityActionType.DOCUMENT_VERSION_CREATED,
                version.getId(), "DOCUMENT_VERSION", null);
        return saved;
    }

    // -------------------------------------------------------------------------
    // LIST VERSIONS  — participation-based read access
    // -------------------------------------------------------------------------

    @GetMapping("/documents/{id}/versions")
    public List<DocumentVersion> listVersions(@PathVariable UUID id,
                                              @AuthenticationPrincipal Jwt jwt,
                                              HttpServletRequest request) {
        UUID actor = actorId(jwt);
        String token = bearerToken(request);

        Document d = documents.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found"));

        requireParticipation(d.getProjectId(), actor, token);
        return this.versions.findByDocumentIdOrderByVersionNumberAsc(d.getId());
    }

    // -------------------------------------------------------------------------
    // ARCHIVE  — owner-only; Document.archive() rejects already-ARCHIVED (→ 409)
    // -------------------------------------------------------------------------

    @PostMapping("/documents/{id}/archive")
    public void archive(@PathVariable UUID id,
                        @AuthenticationPrincipal Jwt jwt,
                        HttpServletRequest request) {
        UUID actor = actorId(jwt);
        String token = bearerToken(request);

        Document d = requireOwner(id, actor, token);
        projectStatusClient.requireOpen(d.getProjectId(), token);
        // Document.archive() throws IllegalStateException (→ 409) when already ARCHIVED
        d.archive();
        documents.save(d);
    }

    // -------------------------------------------------------------------------
    // Authorization helpers
    // -------------------------------------------------------------------------

    /**
     * Verifies the actor's org has ACTIVE participation in {@code projectId}.
     * Throws {@link AccessDeniedException} (→ 403) when no active participation is found,
     * and {@link ServiceUnavailableException} (→ 503) when a downstream call fails.
     */
    private void requireParticipation(UUID projectId, UUID actor, String bearerToken) {
        // Resolve actor's active org IDs
        List<MembershipSummary> memberships = membershipClient.getActiveMemberships(actor, bearerToken);
        if (memberships == null || memberships.isEmpty()) {
            throw new AccessDeniedException("Access denied: no active organization membership");
        }

        Set<UUID> actorOrgIds = memberships.stream()
                .map(MembershipSummary::organizationId)
                .collect(Collectors.toSet());

        // Resolve project's active participating org IDs
        List<ParticipationSummary> participations =
                participationClient.getActiveParticipations(projectId, bearerToken);

        boolean participates = participations != null && participations.stream()
                .anyMatch(p -> actorOrgIds.contains(p.organizationId()));

        if (!participates) {
            throw new AccessDeniedException("Access denied: organization is not an active participant in this project");
        }
    }

    /**
     * Fetches the document, verifies the actor's org participates in its project,
     * and then checks owner identity. Returns the document on success.
     *
     * @throws NoSuchElementException  when the document does not exist
     * @throws AccessDeniedException   when actor has no participation or is not the owner
     */
    private Document requireOwner(UUID documentId, UUID actor, String bearerToken) {
        Document d = documents.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document not found"));

        // Must participate in the project first
        requireParticipation(d.getProjectId(), actor, bearerToken);

        // Then must be the document owner
        if (!d.getCreatedBy().equals(actor)) {
            throw new AccessDeniedException("Document access denied: not the document owner");
        }
        return d;
    }

    // -------------------------------------------------------------------------
    // Request records
    // -------------------------------------------------------------------------

    public record DocumentRequest(
            @NotBlank @Size(max = 240) String title,
            String description) {}

    public record VersionRequest(@Size(max = 500) String changeSummary) {}
}
