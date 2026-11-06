package com.digitaloffice.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, UUID> {

    List<MeetingParticipant> findByMeetingId(UUID meetingId);

    Optional<MeetingParticipant> findByMeetingIdAndUserId(UUID meetingId, UUID userId);

    List<MeetingParticipant> findByMeetingIdAndStatus(UUID meetingId, ParticipantStatus status);

    List<MeetingParticipant> findByUserIdAndStatusIn(UUID userId, List<ParticipantStatus> statuses);

    long countByMeetingIdAndStatusIn(UUID meetingId, List<ParticipantStatus> statuses);
}
