package com.digitaloffice.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findByOrganizationId(UUID organizationId);
    List<Refund> findByPaymentId(UUID paymentId);
}
