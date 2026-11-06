package com.digitaloffice.document;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="review_assignments", uniqueConstraints=@UniqueConstraint(columnNames={"document_version_id","reviewer_id"}))
public class ReviewAssignment {
 @Id private UUID id; @Column(name="document_version_id",nullable=false) private UUID documentVersionId;
 @Column(nullable=false) private UUID reviewerId; @Column(nullable=false) private UUID assignedBy; @Column(nullable=false) private Instant assignedAt;
 protected ReviewAssignment(){}
 public ReviewAssignment(UUID v,UUID r,UUID a){id=UUID.randomUUID();documentVersionId=v;reviewerId=r;assignedBy=a;assignedAt=Instant.now();}
 public UUID getId(){return id;} public UUID getDocumentVersionId(){return documentVersionId;} public UUID getReviewerId(){return reviewerId;} public UUID getAssignedBy(){return assignedBy;} public Instant getAssignedAt(){return assignedAt;}
}
