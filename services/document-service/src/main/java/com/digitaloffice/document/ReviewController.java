package com.digitaloffice.document;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/v1/documents/{docId}/versions/{versionId}")
public class ReviewController {
    private final ReviewService service; private final ReviewAssignmentRepository assignments; private final DocumentReviewRepository reviews;
    public ReviewController(ReviewService service, ReviewAssignmentRepository assignments, DocumentReviewRepository reviews) {
        this.service=service; this.assignments=assignments; this.reviews=reviews;
    }
    @PostMapping("/submit-for-review")
    public DocumentVersion submit(@PathVariable UUID docId,@PathVariable UUID versionId,@AuthenticationPrincipal Jwt jwt,
                                  @RequestHeader("Authorization") String token,@Valid @RequestBody ReviewerRequest r) {
        return service.submitForReview(docId,versionId,r.reviewerIds(),actor(jwt),token);
    }
    @PostMapping("/reviewers") @ResponseStatus(HttpStatus.CREATED)
    public ReviewAssignment assign(@PathVariable UUID docId,@PathVariable UUID versionId,@AuthenticationPrincipal Jwt jwt,
                                   @RequestHeader("Authorization") String token,@Valid @RequestBody ReviewerRequest r) {
        if (r.reviewerIds() == null || r.reviewerIds().size() != 1) throw new IllegalArgumentException("Exactly one reviewer is required");
        return service.assignReviewer(docId,versionId,r.reviewerIds().get(0),actor(jwt),token);
    }
    @GetMapping("/reviewers") public List<ReviewAssignment> reviewers(@PathVariable UUID docId, @PathVariable UUID versionId,
            @AuthenticationPrincipal Jwt jwt, @RequestHeader("Authorization") String token) {
        service.requireProjectAccess(docId, versionId, actor(jwt), token);
        return assignments.findByDocumentVersionId(versionId);
    }
    @PostMapping("/reviews") @ResponseStatus(HttpStatus.CREATED)
    public DocumentReview decide(@PathVariable UUID docId,@PathVariable UUID versionId,@AuthenticationPrincipal Jwt jwt,
                                 @RequestHeader("Authorization") String token,@Valid @RequestBody DecisionRequest r) {
        return service.submitDecision(docId,versionId,actor(jwt),r.decision(),r.feedback(),token);
    }
    @GetMapping("/reviews") public List<DocumentReview> decisions(@PathVariable UUID docId, @PathVariable UUID versionId,
            @AuthenticationPrincipal Jwt jwt, @RequestHeader("Authorization") String token) {
        service.requireProjectAccess(docId, versionId, actor(jwt), token);
        return reviews.findByDocumentVersionId(versionId);
    }
    private static UUID actor(Jwt jwt){return UUID.nameUUIDFromBytes(("oidc:"+jwt.getSubject()).getBytes(StandardCharsets.UTF_8));}
    public record ReviewerRequest(@NotEmpty List<UUID> reviewerIds){}
    public record DecisionRequest(@NotNull ReviewDecision decision,@Size(max=2000) String feedback){}
}
