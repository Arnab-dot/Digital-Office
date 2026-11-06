package com.digitaloffice.organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*; public interface InvitationRepository extends JpaRepository<OrganizationInvitation,UUID>{List<OrganizationInvitation> findByOrganizationId(UUID o);Optional<OrganizationInvitation> findByIdAndInviteeEmail(UUID id,String email);}
