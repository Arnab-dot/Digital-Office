package com.digitaloffice.communication;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meetings", uniqueConstraints = @UniqueConstraint(name = "uk_meetings_room_code", columnNames = "room_code"))
public class Meeting {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "room_code", nullable = false)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", nullable = false)
    private MeetingType meetingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "scheduled_start")
    private Instant scheduledStart;

    @Column(name = "scheduled_end")
    private Instant scheduledEnd;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Column(name = "actual_end")
    private Instant actualEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private int version;

    protected Meeting() {}

    public Meeting(String title, String description, MeetingType meetingType, int maxParticipants,
                   UUID organizationId, UUID projectId, UUID createdBy,
                   Instant scheduledStart, Instant scheduledEnd) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.roomCode = "do-" + UUID.randomUUID().toString().substring(0, 12);
        this.meetingType = meetingType;
        this.maxParticipants = maxParticipants;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.createdBy = createdBy;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.status = meetingType == MeetingType.INSTANT ? MeetingStatus.ACTIVE : MeetingStatus.SCHEDULED;
        this.actualStart = meetingType == MeetingType.INSTANT ? Instant.now() : null;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Getters
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRoomCode() { return roomCode; }
    public MeetingType getMeetingType() { return meetingType; }
    public MeetingStatus getStatus() { return status; }
    public int getMaxParticipants() { return maxParticipants; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getProjectId() { return projectId; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getScheduledStart() { return scheduledStart; }
    public Instant getScheduledEnd() { return scheduledEnd; }
    public Instant getActualStart() { return actualStart; }
    public Instant getActualEnd() { return actualEnd; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    // State transitions
    public void update(String title, String description, Instant scheduledStart, Instant scheduledEnd) {
        if (status == MeetingStatus.COMPLETED || status == MeetingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a " + status.name().toLowerCase() + " meeting");
        }
        this.title = title;
        this.description = description;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.updatedAt = Instant.now();
    }

    public void start() {
        if (status != MeetingStatus.SCHEDULED) {
            throw new IllegalStateException("Only scheduled meetings can be started");
        }
        this.status = MeetingStatus.ACTIVE;
        this.actualStart = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void end() {
        if (status != MeetingStatus.ACTIVE) {
            throw new IllegalStateException("Only active meetings can be ended");
        }
        this.status = MeetingStatus.COMPLETED;
        this.actualEnd = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (status == MeetingStatus.COMPLETED || status == MeetingStatus.CANCELLED) {
            throw new IllegalStateException("Meeting is already " + status.name().toLowerCase());
        }
        this.status = MeetingStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
