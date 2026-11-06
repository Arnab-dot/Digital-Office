package com.digitaloffice.work;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface WorkstreamRepository extends JpaRepository<Workstream, UUID> {
    List<Workstream> findByProjectIdAndStatus(UUID projectId, WorkstreamStatus status);
}
