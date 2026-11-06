package com.digitaloffice.work;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {
    List<ActivityEvent> findByProjectIdOrderByEventAtDesc(UUID projectId);
    List<ActivityEvent> findBySubjectIdOrderByEventAtDesc(UUID subjectId);
    Page<ActivityEvent> findByProjectIdOrderByEventAtDesc(UUID projectId, Pageable pageable);
}
