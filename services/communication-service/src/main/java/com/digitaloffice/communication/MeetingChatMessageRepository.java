package com.digitaloffice.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MeetingChatMessageRepository extends JpaRepository<MeetingChatMessage, UUID> {

    List<MeetingChatMessage> findByMeetingIdOrderByCreatedAtAsc(UUID meetingId);
}
