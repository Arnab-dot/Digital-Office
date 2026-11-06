package com.digitaloffice.document;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="document_reviews", uniqueConstraints=@UniqueConstraint(columnNames={"document_version_id","reviewer_id"}))
public class DocumentReview {
 @Id private UUID id; @Column(name="document_version_id",nullable=false) private UUID documentVersionId;
 @Column(nullable=false) private UUID reviewerId; @Enumerated(EnumType.STRING) @Column(nullable=false) private ReviewDecision decision;
 private String feedback; @Column(nullable=false) private Instant decidedAt;
 protected DocumentReview(){}
 public DocumentReview(UUID v,UUID r,ReviewDecision d,String f){id=UUID.randomUUID();documentVersionId=v;reviewerId=r;decision=d;feedback=f;decidedAt=Instant.now();}
 public UUID getId(){return id;} public UUID getDocumentVersionId(){return documentVersionId;} public UUID getReviewerId(){return reviewerId;} public ReviewDecision getDecision(){return decision;} public String getFeedback(){return feedback;} public Instant getDecidedAt(){return decidedAt;}
}
