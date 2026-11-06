package com.digitaloffice.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InvitationEmailRepository extends JpaRepository<InvitationEmail, UUID> {
    List<InvitationEmail> findByOrganizationIdOrderBySentAtDesc(UUID organizationId);
}

