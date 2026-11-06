package com.digitaloffice.work;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import java.util.*;

@Service
public class ActivityService {
    private final ActivityEventRepository events;
    public ActivityService(ActivityEventRepository events) { this.events = events; }
    public ActivityEvent recordEvent(UUID projectId, UUID actorId, WorkActivityActionType type,
                                     UUID subjectId, String subjectType, String metadataJson) {
        if (projectId == null || actorId == null || type == null || subjectId == null
                || subjectType == null || subjectType.isBlank()) {
            throw new IllegalArgumentException("Activity event fields are required");
        }
        return events.save(new ActivityEvent(projectId, actorId, type, subjectId, subjectType, metadataJson));
    }
    public List<ActivityEvent> findByProject(UUID projectId) { return events.findByProjectIdOrderByEventAtDesc(projectId); }
    public List<ActivityEvent> findBySubject(UUID subjectId) { return events.findBySubjectIdOrderByEventAtDesc(subjectId); }
    public Page<ActivityEvent> eventsPage(UUID projectId, Pageable pageable) {
        return events.findByProjectIdOrderByEventAtDesc(projectId, pageable);
    }
}
