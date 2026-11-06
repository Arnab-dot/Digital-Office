package com.digitaloffice.work;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CommentRepository extends JpaRepository<TaskComment, UUID> {
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
