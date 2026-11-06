package com.digitaloffice.communication;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_chat_messages")
public class MeetingChatMessage {

    @Id
    private UUID id;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String content;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MeetingChatMessage() {}

    public MeetingChatMessage(UUID meetingId, UUID senderId, String senderName, String content) {
        this.id = UUID.randomUUID();
        this.meetingId = meetingId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.messageType = "TEXT";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getMeetingId() { return meetingId; }
    public UUID getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public String getMessageType() { return messageType; }
    public Instant getCreatedAt() { return createdAt; }
}
