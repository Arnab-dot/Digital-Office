package com.digitaloffice.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findByOrganizationId(UUID organizationId, Pageable pageable);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
