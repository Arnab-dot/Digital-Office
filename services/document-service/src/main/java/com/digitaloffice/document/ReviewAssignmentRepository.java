package com.digitaloffice.document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment,UUID>{
 List<ReviewAssignment> findByDocumentVersionId(UUID id);
 List<ReviewAssignment> findByReviewerId(UUID reviewerId);
 boolean existsByDocumentVersionIdAndReviewerId(UUID versionId,UUID reviewerId);
 long countByDocumentVersionId(UUID versionId);
}
