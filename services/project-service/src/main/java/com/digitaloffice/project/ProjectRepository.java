package com.digitaloffice.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOwnerOrganizationId(UUID id);

    /**
     * Returns all projects where the ownerOrganizationId is in the given collection,
     * used to find projects owned by any of the actor's organizations.
     */
    List<Project> findByOwnerOrganizationIdIn(Collection<UUID> organizationIds);

    /**
     * Returns all projects accessible to the actor: projects whose owning org the actor
     * belongs to OR where the actor is the creator.
     * Used by {@code ProjectService.listAccessible}.
     */
    @Query("SELECT DISTINCT p FROM Project p WHERE p.ownerOrganizationId IN :orgIds OR p.createdBy = :actorId")
    List<Project> findByOwnerOrganizationIdInOrCreatedBy(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("actorId") UUID actorId);
    List<Project> findByNameContainingIgnoreCaseAndOwnerOrganizationIdIn(String keyword, Collection<UUID> organizationIds);

    @Query("SELECT p FROM Project p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND (p.id IN :projectIds OR p.createdBy = :actorId)")
    List<Project> searchAccessible(@Param("keyword") String keyword,
                                   @Param("projectIds") Collection<UUID> projectIds,
                                   @Param("actorId") UUID actorId);
}
