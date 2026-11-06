package com.digitaloffice.work;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, UUID> {
    List<TaskDependency> findByTaskId(UUID taskId);
    List<TaskDependency> findByTaskIdIn(Collection<UUID> taskIds);
    boolean existsByTaskIdAndDependsOnTaskId(UUID taskId, UUID dependsOnTaskId);
    Optional<TaskDependency> findByTaskIdAndDependsOnTaskId(UUID taskId, UUID dependsOnTaskId);
}
