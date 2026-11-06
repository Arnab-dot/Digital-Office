package com.digitaloffice.organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*; public interface OrganizationRepository extends JpaRepository<Organization,UUID>{Optional<Organization> findBySlug(String slug);}
