package com.digitaloffice.document;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface DocumentActivityEventRepository extends JpaRepository<DocumentActivityEvent,UUID>{List<DocumentActivityEvent> findByProjectIdOrderByEventAtDesc(UUID id);List<DocumentActivityEvent> findBySubjectIdOrderByEventAtDesc(UUID id);}
