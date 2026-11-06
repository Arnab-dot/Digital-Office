package com.digitaloffice.work;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByProjectId(UUID projectId);
    long countByAssigneeIdAndStatusIn(UUID assigneeId, Collection<TaskStatus> statuses);

    @Query("select t from Task t where lower(t.title) like lower(concat('%', :keyword, '%')) and t.projectId in :projects")
    List<Task> searchByTitle(@Param("keyword") String keyword, @Param("projects") Collection<UUID> projects);
}
