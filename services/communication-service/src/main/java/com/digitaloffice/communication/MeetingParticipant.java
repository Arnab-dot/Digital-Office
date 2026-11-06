package com.digitaloffice.communication;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_participants",
       uniqueConstraints = @UniqueConstraint(name = "uk_meeting_participant_user", columnNames = {"meeting_id", "user_id"}))
public class MeetingParticipant {

    @Id
    private UUID id;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MeetingParticipant() {}

    public MeetingParticipant(UUID meetingId, UUID userId, String displayName, String email,
                              ParticipantRole role, UUID invitedBy) {
        this.id = UUID.randomUUID();
        this.meetingId = meetingId;
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
        this.status = role == ParticipantRole.ORGANIZER ? ParticipantStatus.ACCEPTED : ParticipantStatus.INVITED;
        this.invitedBy = invitedBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getMeetingId() { return meetingId; }
    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public ParticipantRole getRole() { return role; }
    public ParticipantStatus getStatus() { return status; }
    public UUID getInvitedBy() { return invitedBy; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // State transitions
    public void join() {
        this.status = ParticipantStatus.JOINED;
        this.joinedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void leave() {
        this.status = ParticipantStatus.LEFT;
        this.leftAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void accept() {
        if (status != ParticipantStatus.INVITED) {
            throw new IllegalStateException("Can only accept an invitation");
        }
        this.status = ParticipantStatus.ACCEPTED;
        this.updatedAt = Instant.now();
    }

    public void decline() {
        if (status != ParticipantStatus.INVITED) {
            throw new IllegalStateException("Can only decline an invitation");
        }
        this.status = ParticipantStatus.DECLINED;
        this.updatedAt = Instant.now();
    }

    public void remove() {
        this.status = ParticipantStatus.REMOVED;
        this.leftAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void setRole(ParticipantRole role) {
        this.role = role;
        this.updatedAt = Instant.now();
    }
}
