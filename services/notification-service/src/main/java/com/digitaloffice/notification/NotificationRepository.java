package com.digitaloffice.notification;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification,UUID>{Page<Notification> findByRecipientId(UUID id,Pageable p); Page<Notification> findByRecipientIdAndReadFlag(UUID id,boolean read,Pageable p); long countByRecipientIdAndReadFlag(UUID id,boolean read);}
