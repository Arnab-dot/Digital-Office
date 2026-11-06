package com.digitaloffice.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    Optional<Meeting> findByRoomCode(String roomCode);

    List<Meeting> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);

    List<Meeting> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<Meeting> findByStatusOrderByScheduledStartAsc(MeetingStatus status);

    List<Meeting> findByStatusInOrderByCreatedAtDesc(List<MeetingStatus> statuses);
}
