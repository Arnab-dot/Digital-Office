package com.digitaloffice.document;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Internal read endpoint used by the dashboard aggregator. */
@RestController
@RequestMapping("/internal/reviews")
public class InternalReviewController {
    private final ReviewAssignmentRepository assignments;
    private final DocumentVersionRepository versions;

    public InternalReviewController(ReviewAssignmentRepository assignments, DocumentVersionRepository versions) {
        this.assignments = assignments;
        this.versions = versions;
    }

    @GetMapping("/pending")
    public List<DocumentVersion> pending(@AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
        var assignedIds = assignments.findByReviewerId(actor).stream()
                .map(ReviewAssignment::getDocumentVersionId).collect(java.util.stream.Collectors.toSet());
        if (assignedIds.isEmpty()) return List.of();
        return versions.findByReviewStatus(DocumentReviewStatus.IN_REVIEW).stream()
                .filter(v -> assignedIds.contains(v.getId())).toList();
    }
}
