package com.digitaloffice.communication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Business logic for meeting lifecycle management with Jitsi Meet integration.
 *
 * <p>Authorization: actor must be the meeting creator or a participant.
 * Organizer/co-host actions are enforced via participant role checks.
 */
@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingChatMessageRepository chatRepository;

    @Value("${jitsi.server-url:https://meet.jit.si}")
    private String jitsiServerUrl;

    public MeetingService(MeetingRepository meetingRepository,
                          MeetingParticipantRepository participantRepository,
                          MeetingChatMessageRepository chatRepository) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.chatRepository = chatRepository;
    }

    // -----------------------------------------------------------------------
    // Meeting CRUD
    // -----------------------------------------------------------------------

    @Transactional
    public Meeting create(String title, String description, MeetingType meetingType,
                          int maxParticipants, UUID organizationId, UUID projectId,
                          UUID actor, String actorName, String actorEmail,
                          Instant scheduledStart, Instant scheduledEnd) {
        if (maxParticipants < 2 || maxParticipants > 100) {
            throw new IllegalArgumentException("Max participants must be between 2 and 100");
        }
        Meeting meeting = meetingRepository.save(
                new Meeting(title, description, meetingType, maxParticipants,
                            organizationId, projectId, actor, scheduledStart, scheduledEnd));

        // Add creator as organizer
        participantRepository.save(
                new MeetingParticipant(meeting.getId(), actor, actorName, actorEmail,
                                      ParticipantRole.ORGANIZER, null));
        return meeting;
    }

    public List<Meeting> listForUser(UUID actor) {
        // Return meetings created by the user + meetings they're a participant in
        List<UUID> participatingMeetingIds = participantRepository
                .findByUserIdAndStatusIn(actor, List.of(
                        ParticipantStatus.INVITED, ParticipantStatus.ACCEPTED,
                        ParticipantStatus.JOINED))
                .stream()
                .map(MeetingParticipant::getMeetingId)
                .toList();

        List<Meeting> created = meetingRepository.findByCreatedByOrderByCreatedAtDesc(actor);
        List<Meeting> participating = meetingRepository.findAllById(participatingMeetingIds);

        // Merge and deduplicate, prioritizing created-by list
        java.util.LinkedHashMap<UUID, Meeting> merged = new java.util.LinkedHashMap<>();
        created.forEach(m -> merged.put(m.getId(), m));
        participating.forEach(m -> merged.putIfAbsent(m.getId(), m));
        return List.copyOf(merged.values());
    }

    public Meeting get(UUID meetingId) {
        return findMeeting(meetingId);
    }

    @Transactional
    public Meeting update(UUID meetingId, String title, String description,
                          Instant scheduledStart, Instant scheduledEnd, UUID actor) {
        Meeting meeting = findMeeting(meetingId);
        requireOrganizerOrCoHost(meeting, actor);
        meeting.update(title, description, scheduledStart, scheduledEnd);
        return meetingRepository.save(meeting);
    }

    @Transactional
    public void cancel(UUID meetingId, UUID actor) {
        Meeting meeting = findMeeting(meetingId);
        requireOrganizerOrCoHost(meeting, actor);
        meeting.cancel();
        meetingRepository.save(meeting);
    }

    @Transactional
    public Meeting start(UUID meetingId, UUID actor) {
        Meeting meeting = findMeeting(meetingId);
        requireOrganizerOrCoHost(meeting, actor);
        meeting.start();
        return meetingRepository.save(meeting);
    }

    @Transactional
    public Meeting end(UUID meetingId, UUID actor) {
        Meeting meeting = findMeeting(meetingId);
        requireOrganizerOrCoHost(meeting, actor);
        meeting.end();
        return meetingRepository.save(meeting);
    }

    // -----------------------------------------------------------------------
    // Join flow — returns Jitsi config for the browser
    // -----------------------------------------------------------------------

    @Transactional
    public JoinResponse join(UUID meetingId, UUID actor, String actorName, String actorEmail) {
        Meeting meeting = findMeeting(meetingId);

        if (meeting.getStatus() != MeetingStatus.ACTIVE && meeting.getStatus() != MeetingStatus.SCHEDULED) {
            throw new IllegalStateException("Meeting is not joinable (status: " + meeting.getStatus() + ")");
        }

        // Auto-start scheduled meetings when first person joins
        if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
            meeting.start();
            meetingRepository.save(meeting);
        }

        // Find or create participant record
        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndUserId(meetingId, actor)
                .orElseGet(() -> {
                    // Check capacity before adding new participant
                    long activeCount = participantRepository.countByMeetingIdAndStatusIn(meetingId,
                            List.of(ParticipantStatus.JOINED, ParticipantStatus.ACCEPTED));
                    if (activeCount >= meeting.getMaxParticipants()) {
                        throw new IllegalStateException("Meeting is at capacity (" + meeting.getMaxParticipants() + " participants)");
                    }
                    return participantRepository.save(
                            new MeetingParticipant(meetingId, actor, actorName, actorEmail,
                                                   ParticipantRole.PARTICIPANT, null));
                });

        participant.join();
        participantRepository.save(participant);

        return new JoinResponse(
                meeting.getId(),
                meeting.getRoomCode(),
                meeting.getTitle(),
                jitsiServerUrl,
                actorName,
                actorEmail,
                participant.getRole().name()
        );
    }

    @Transactional
    public void leave(UUID meetingId, UUID actor) {
        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndUserId(meetingId, actor)
                .orElseThrow(() -> new NoSuchElementException("Not a participant in this meeting"));
        participant.leave();
        participantRepository.save(participant);
    }

    // -----------------------------------------------------------------------
    // Participant management
    // -----------------------------------------------------------------------

    public List<MeetingParticipant> participants(UUID meetingId) {
        findMeeting(meetingId); // validate exists
        return participantRepository.findByMeetingId(meetingId);
    }

    @Transactional
    public MeetingParticipant invite(UUID meetingId, UUID userId, String displayName,
                                     String email, UUID actor) {
        Meeting meeting = findMeeting(meetingId);
        requireOrganizerOrCoHost(meeting, actor);

        // Check for duplicate
        if (participantRepository.findByMeetingIdAndUserId(meetingId, userId).isPresent()) {
            throw new IllegalArgumentException("User is already a participant");
        }

        // Check capacity
        long count = participantRepository.countByMeetingIdAndStatusIn(meetingId,
                List.of(ParticipantStatus.INVITED, ParticipantStatus.ACCEPTED, ParticipantStatus.JOINED));
        if (count >= meeting.getMaxParticipants()) {
            throw new IllegalStateException("Meeting is at capacity");
        }

        return participantRepository.save(
                new MeetingParticipant(meetingId, userId, displayName, email,
                                       ParticipantRole.PARTICIPANT, actor));
    }

    @Transactional
    public void removeParticipant(UUID meetingId, UUID participantId, UUID actor) {
        Meeting meeting = findMeeting(meetingId);
        requireOrganizerOrCoHost(meeting, actor);
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new NoSuchElementException("Participant not found"));
        if (!participant.getMeetingId().equals(meetingId)) {
            throw new IllegalArgumentException("Participant does not belong to this meeting");
        }
        if (participant.getRole() == ParticipantRole.ORGANIZER) {
            throw new IllegalArgumentException("Cannot remove the meeting organizer");
        }
        participant.remove();
        participantRepository.save(participant);
    }

    @Transactional
    public MeetingParticipant promoteToCoHost(UUID meetingId, UUID participantId, UUID actor) {
        Meeting meeting = findMeeting(meetingId);
        requireOrganizer(meeting, actor);
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new NoSuchElementException("Participant not found"));
        participant.setRole(ParticipantRole.CO_HOST);
        return participantRepository.save(participant);
    }

    // -----------------------------------------------------------------------
    // Chat
    // -----------------------------------------------------------------------

    @Transactional
    public MeetingChatMessage sendMessage(UUID meetingId, UUID senderId, String senderName, String content) {
        Meeting meeting = findMeeting(meetingId);
        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            throw new IllegalStateException("Can only send messages in active meetings");
        }
        return chatRepository.save(new MeetingChatMessage(meetingId, senderId, senderName, content));
    }

    public List<MeetingChatMessage> chatHistory(UUID meetingId) {
        findMeeting(meetingId);
        return chatRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
    }

    // -----------------------------------------------------------------------
    // Authorization helpers
    // -----------------------------------------------------------------------

    private void requireOrganizerOrCoHost(Meeting meeting, UUID actor) {
        if (meeting.getCreatedBy().equals(actor)) return;
        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndUserId(meeting.getId(), actor)
                .orElseThrow(() -> new AccessDeniedException("Not a participant in this meeting"));
        if (participant.getRole() != ParticipantRole.ORGANIZER &&
            participant.getRole() != ParticipantRole.CO_HOST) {
            throw new AccessDeniedException("Only organizers and co-hosts can perform this action");
        }
    }

    private void requireOrganizer(Meeting meeting, UUID actor) {
        if (meeting.getCreatedBy().equals(actor)) return;
        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndUserId(meeting.getId(), actor)
                .orElseThrow(() -> new AccessDeniedException("Not a participant"));
        if (participant.getRole() != ParticipantRole.ORGANIZER) {
            throw new AccessDeniedException("Only the organizer can perform this action");
        }
    }

    private Meeting findMeeting(UUID meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NoSuchElementException("Meeting not found: " + meetingId));
    }

    // -----------------------------------------------------------------------
    // Response records
    // -----------------------------------------------------------------------

    public record JoinResponse(UUID meetingId, String roomCode, String title,
                                String jitsiServerUrl, String displayName,
                                String email, String role) {}
}
