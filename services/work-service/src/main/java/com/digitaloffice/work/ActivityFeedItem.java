package com.digitaloffice.work;

import java.time.Instant;
import java.util.UUID;

public record ActivityFeedItem(UUID id, UUID projectId, UUID actorId, String actionType,
                               UUID subjectId, String subjectType, String metadata, Instant eventAt) {}
