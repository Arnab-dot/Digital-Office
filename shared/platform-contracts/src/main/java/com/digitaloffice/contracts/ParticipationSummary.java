package com.digitaloffice.contracts;

import java.util.UUID;

/**
 * Projection of an active project participation returned by Project_Service's
 * internal endpoint {@code GET /internal/participations?projectId={uuid}}.
 *
 * @param organizationId the organization that participates in the project
 * @param role           the organization's role within that project
 */
public record ParticipationSummary(UUID organizationId, ProjectParticipationRole role) {}
