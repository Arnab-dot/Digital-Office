package com.digitaloffice.document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DocumentReviewRepository extends JpaRepository<DocumentReview,UUID>{
 List<DocumentReview> findByDocumentVersionId(UUID id);
 Optional<DocumentReview> findByDocumentVersionIdAndReviewerId(UUID versionId,UUID reviewerId);
 long countByDocumentVersionIdAndDecision(UUID versionId,ReviewDecision decision);
}
