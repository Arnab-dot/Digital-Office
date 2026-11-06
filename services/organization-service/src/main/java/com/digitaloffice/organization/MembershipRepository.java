package com.digitaloffice.organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*; public interface MembershipRepository extends JpaRepository<OrganizationMembership,UUID>{Optional<OrganizationMembership> findByOrganizationIdAndUserId(UUID o,UUID u);List<OrganizationMembership> findByUserIdAndStatus(UUID u,MembershipStatus s);List<OrganizationMembership> findByOrganizationId(UUID o);long countByOrganizationIdAndRoleAndStatus(UUID o,OrganizationRole r,MembershipStatus s);}
