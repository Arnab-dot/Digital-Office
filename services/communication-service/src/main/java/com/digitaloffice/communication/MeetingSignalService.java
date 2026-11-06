package com.digitaloffice.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class MeetingSignalService {

    private static final Logger log = LoggerFactory.getLogger(MeetingSignalService.class);
    private static final long SIGNAL_TTL_MS = 60_000L; // 60 seconds

    private final Map<String, ConcurrentLinkedDeque<MeetingSignal>> meetingSignals = new ConcurrentHashMap<>();

    public MeetingSignal broadcast(String meetingId, String type, String senderPeerId,
                                   String senderName, String targetPeerId, Object payload) {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        MeetingSignal signal = new MeetingSignal(
                id, meetingId, type, senderPeerId, senderName, targetPeerId, payload, now
        );

        meetingSignals.compute(meetingId, (k, deque) -> {
            if (deque == null) {
                deque = new ConcurrentLinkedDeque<>();
            }
            long cutoff = now - SIGNAL_TTL_MS;
            while (!deque.isEmpty() && deque.peekFirst().timestamp() < cutoff) {
                deque.pollFirst();
            }
            deque.addLast(signal);
            return deque;
        });

        return signal;
    }

    public List<MeetingSignal> getSignals(String meetingId, String peerId, long since) {
        ConcurrentLinkedDeque<MeetingSignal> deque = meetingSignals.get(meetingId);
        if (deque == null || deque.isEmpty()) {
            return Collections.emptyList();
        }

        List<MeetingSignal> result = new ArrayList<>();
        for (MeetingSignal s : deque) {
            if (s.timestamp() > since) {
                if (peerId != null && peerId.equals(s.senderPeerId())) {
                    continue;
                }
                if (s.targetPeerId() == null || (peerId != null && peerId.equals(s.targetPeerId()))) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    public void cleanUp(String meetingId) {
        meetingSignals.remove(meetingId);
    }
}

