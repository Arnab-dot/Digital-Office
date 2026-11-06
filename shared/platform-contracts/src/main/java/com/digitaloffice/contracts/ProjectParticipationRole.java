package com.digitaloffice.contracts;

/**
 * Minimal self-contained copy of project participation roles used in cross-service contracts.
 *
 * <p>Mirrors {@code com.digitaloffice.project.ProjectRole} but lives in
 * {@code platform-contracts} so services that depend on this library do not need a
 * compile-time dependency on project-service.
 */
public enum ProjectParticipationRole {
    PROJECT_OWNER,
    PROJECT_ADMIN,
    COLLABORATOR,
    VIEWER
}
