package com.digitaloffice.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByOrganizationId(UUID organizationId, Pageable pageable);
    List<Invoice> findByOrganizationIdAndStatus(UUID organizationId, InvoiceStatus status);
    long countByOrganizationId(UUID organizationId);
}
