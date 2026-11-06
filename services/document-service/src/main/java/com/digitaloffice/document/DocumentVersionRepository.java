package com.digitaloffice.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberAsc(UUID documentId);
    List<DocumentVersion> findByReviewStatus(DocumentReviewStatus status);
}
