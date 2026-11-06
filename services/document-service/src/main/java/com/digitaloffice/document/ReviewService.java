package com.digitaloffice.document;

import com.digitaloffice.contracts.MembershipSummary;
import com.digitaloffice.contracts.DomainEventPublisher;
import com.digitaloffice.contracts.UserNotificationEvent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ReviewService {
    private final DocumentVersionRepository versions;
    private final DocumentRepository documents;
    private final ReviewAssignmentRepository assignments;
    private final DocumentReviewRepository reviews;
    private final OrganizationMembershipClient memberships;
    private final ProjectParticipationClient participations;
    private final ProjectStatusClient projectStatus;
    private final DocumentActivityService activity;
    private final DomainEventPublisher domainEvents;

    public ReviewService(DocumentVersionRepository versions, DocumentRepository documents,
                         ReviewAssignmentRepository assignments, DocumentReviewRepository reviews,
                         OrganizationMembershipClient memberships, ProjectParticipationClient participations,
                         ProjectStatusClient projectStatus, DocumentActivityService activity,
                         DomainEventPublisher domainEvents) {
        this.versions = versions; this.documents = documents; this.assignments = assignments; this.reviews = reviews;
        this.memberships = memberships; this.participations = participations;
        this.projectStatus = projectStatus;
        this.activity = activity;
        this.domainEvents = domainEvents;
    }
    @Transactional
    public DocumentVersion submitForReview(UUID docId, UUID versionId, List<UUID> reviewerIds, UUID actor, String token) {
        DocumentVersion version = loadVersion(docId, versionId);
        if (!actor.equals(version.getCreatedBy())) throw new AccessDeniedException("Only the owner can submit for review");
        if (version.getReviewStatus() != DocumentReviewStatus.DRAFT) throw new IllegalStateException("Version is not a draft");
        if (reviewerIds == null || reviewerIds.size() < 1 || reviewerIds.size() > 25)
            throw new IllegalArgumentException("Reviewer count must be between 1 and 25");
        UUID projectId = project(docId);
        projectStatus.requireOpen(projectId, token);
        for (UUID reviewer : new HashSet<>(reviewerIds)) {
            if (!reviewerParticipates(projectId, reviewer, token)) throw new AccessDeniedException("Reviewer is not a project participant");
            assignments.save(new ReviewAssignment(versionId, reviewer, actor));
        }
        version.setReviewStatus(DocumentReviewStatus.IN_REVIEW);
        DocumentVersion saved = versions.save(version);
        activity.recordEvent(projectId, actor, DocumentActivityActionType.REVIEW_REQUESTED,
                versionId, "DOCUMENT_VERSION", "{\"reviewerCount\":" + new HashSet<>(reviewerIds).size() + "}");
        for (UUID reviewer : new HashSet<>(reviewerIds)) {
            domainEvents.publish(new UserNotificationEvent("REVIEW_REQUESTED", versionId, actor,
                    java.time.Instant.now(), reviewer, "DOCUMENT_VERSION", "{\"documentId\":\"" + docId + "\"}"));
        }
        return saved;
    }
    @Transactional
    public ReviewAssignment assignReviewer(UUID docId, UUID versionId, UUID reviewer, UUID actor, String token) {
        DocumentVersion version = loadVersion(docId, versionId);
        if (!actor.equals(version.getCreatedBy())) throw new AccessDeniedException("Only the owner can assign reviewers");
        projectStatus.requireOpen(project(docId), token);
        if (version.getReviewStatus() != DocumentReviewStatus.IN_REVIEW) throw new IllegalStateException("Version is not in review");
        if (!reviewerParticipates(project(docId), reviewer, token)) throw new AccessDeniedException("Reviewer is not a project participant");
        if (assignments.existsByDocumentVersionIdAndReviewerId(versionId, reviewer)) throw new IllegalStateException("Reviewer already assigned");
        ReviewAssignment assignment = assignments.save(new ReviewAssignment(versionId, reviewer, actor));
        activity.recordEvent(project(docId), actor, DocumentActivityActionType.REVIEW_REQUESTED,
                versionId, "DOCUMENT_VERSION", "{\"reviewerAdded\":\"" + reviewer + "\"}");
        return assignment;
    }
    @Transactional
    public DocumentReview submitDecision(UUID docId, UUID versionId, UUID actor, ReviewDecision decision,
                                         String feedback, String token) {
        DocumentVersion version = loadVersion(docId, versionId);
        projectStatus.requireOpen(project(docId), token);
        if (version.getReviewStatus() != DocumentReviewStatus.IN_REVIEW) throw new IllegalStateException("Version is not in review");
        if (actor.equals(version.getCreatedBy()) || !assignments.existsByDocumentVersionIdAndReviewerId(versionId, actor))
            throw new AccessDeniedException("Actor is not an assigned reviewer");
        if (reviews.findByDocumentVersionIdAndReviewerId(versionId, actor).isPresent())
            throw new IllegalStateException("Reviewer has already decided");
        if (decision == ReviewDecision.REJECTED && (feedback == null || feedback.trim().isEmpty() || feedback.trim().length() > 2000))
            throw new IllegalArgumentException("Rejection feedback must be 1-2000 characters");
        DocumentReview saved = reviews.save(new DocumentReview(versionId, actor, decision, feedback == null ? null : feedback.trim()));
        if (decision == ReviewDecision.REJECTED) version.setReviewStatus(DocumentReviewStatus.REJECTED);
        else {
            long total = assignments.countByDocumentVersionId(versionId);
            long approved = reviews.countByDocumentVersionIdAndDecision(versionId, ReviewDecision.APPROVED);
            if (total > 0 && approved == total) version.setReviewStatus(DocumentReviewStatus.APPROVED);
        }
        versions.save(version);
        activity.recordEvent(project(docId), actor,
                decision == ReviewDecision.APPROVED ? DocumentActivityActionType.REVIEW_APPROVED : DocumentActivityActionType.REVIEW_REJECTED,
                versionId, "DOCUMENT_VERSION", "{\"decision\":\"" + decision + "\"}");
        if (version.getReviewStatus() == DocumentReviewStatus.APPROVED || version.getReviewStatus() == DocumentReviewStatus.REJECTED) {
            domainEvents.publish(new UserNotificationEvent("DOCUMENT_REVIEW_COMPLETED", versionId, actor,
                    java.time.Instant.now(), version.getCreatedBy(), "DOCUMENT_VERSION",
                    "{\"status\":\"" + version.getReviewStatus() + "\"}"));
        }
        return saved;
    }

    public void requireProjectAccess(UUID docId, UUID versionId, UUID actor, String token) {
        loadVersion(docId, versionId);
        UUID projectId = project(docId);
        boolean allowed = memberships.getActiveMemberships(actor, token).stream()
                .anyMatch(m -> participations.hasActiveParticipation(projectId, m.organizationId(), token));
        if (!allowed) throw new AccessDeniedException("Actor cannot access document review history");
    }
    private DocumentVersion loadVersion(UUID docId, UUID versionId) {
        DocumentVersion v = versions.findById(versionId).orElseThrow(() -> new NoSuchElementException("Version not found"));
        if (!v.getDocumentId().equals(docId)) throw new NoSuchElementException("Version not found");
        return v;
    }
    private UUID project(UUID docId) {
        return documents.findById(docId).orElseThrow(() -> new NoSuchElementException("Document not found")).getProjectId();
    }
    private boolean reviewerParticipates(UUID projectId, UUID reviewer, String token) {
        return memberships.getActiveMemberships(reviewer, token).stream()
                .anyMatch(m -> participations.hasActiveParticipation(projectId, m.organizationId(), token));
    }
}
