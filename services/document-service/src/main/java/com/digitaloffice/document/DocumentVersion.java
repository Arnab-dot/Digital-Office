package com.digitaloffice.document;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_document_version_number",
                columnNames = {"document_id", "version_number"}))
public class DocumentVersion {
    @Id private UUID id;
    @Column(name = "document_id", nullable = false) private UUID documentId;
    @Column(name = "version_number", nullable = false) private int versionNumber;
    @Column(nullable = false) private UUID createdBy;
    private String changeSummary;
    private String storageReference;
    @Enumerated(EnumType.STRING) @Column(name = "review_status", nullable = false)
    private DocumentReviewStatus reviewStatus = DocumentReviewStatus.DRAFT;
    @Column(nullable = false) private Instant createdAt;

    protected DocumentVersion() {}
    public DocumentVersion(UUID documentId, int versionNumber, UUID createdBy, String changeSummary) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.createdBy = createdBy;
        this.changeSummary = changeSummary;
        this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public int getVersionNumber() { return versionNumber; }
    public UUID getCreatedBy() { return createdBy; }
    public String getChangeSummary() { return changeSummary; }
    public String getStorageReference() { return storageReference; }
    public Instant getCreatedAt() { return createdAt; }
    public DocumentReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(DocumentReviewStatus status) { this.reviewStatus = status; }
    public void ensureNotApproved() {
        if (reviewStatus == DocumentReviewStatus.APPROVED) throw new IllegalStateException("Approved version is immutable");
    }
}
