package com.digitaloffice.communication;

public record MeetingSignal(
        String id,
        String meetingId,
        String type,
        String senderPeerId,
        String senderName,
        String targetPeerId,
        Object payload,
        long timestamp
) {}

