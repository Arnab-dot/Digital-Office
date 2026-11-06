package com.digitaloffice.communication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for video conferencing meeting management.
 * Follows the same patterns as ProjectController — JWT auth, inner records,
 * delegation to service layer, actor derivation from JWT subject.
 */
@RestController
@RequestMapping("/api/v1")
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingSignalService signalService;

    public MeetingController(MeetingService meetingService, MeetingSignalService signalService) {
        this.meetingService = meetingService;
        this.signalService = signalService;
    }

    // -----------------------------------------------------------------------
    // Meeting CRUD
    // -----------------------------------------------------------------------

    @PostMapping("/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    public Meeting create(@AuthenticationPrincipal Jwt jwt,
                          @Valid @RequestBody CreateMeetingRequest r) {
        return meetingService.create(
                r.title(), r.description(), r.meetingType(),
                r.maxParticipants() != null ? r.maxParticipants() : 50,
                r.organizationId(), r.projectId(),
                actor(jwt), displayName(jwt), email(jwt),
                r.scheduledStart(), r.scheduledEnd());
    }

    @GetMapping("/meetings")
    public List<Meeting> list(@AuthenticationPrincipal Jwt jwt) {
        return meetingService.listForUser(actor(jwt));
    }

    @GetMapping("/meetings/{id}")
    public Meeting get(@PathVariable UUID id) {
        return meetingService.get(id);
    }

    @PatchMapping("/meetings/{id}")
    public Meeting update(@PathVariable UUID id,
                          @AuthenticationPrincipal Jwt jwt,
                          @Valid @RequestBody UpdateMeetingRequest r) {
        return meetingService.update(id, r.title(), r.description(),
                r.scheduledStart(), r.scheduledEnd(), actor(jwt));
    }

    @PostMapping("/meetings/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        meetingService.cancel(id, actor(jwt));
    }

    @PostMapping("/meetings/{id}/start")
    public Meeting start(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return meetingService.start(id, actor(jwt));
    }

    @PostMapping("/meetings/{id}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void end(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        meetingService.end(id, actor(jwt));
    }

    // -----------------------------------------------------------------------
    // Join / Leave
    // -----------------------------------------------------------------------

    @PostMapping("/meetings/{id}/join")
    public MeetingService.JoinResponse join(@PathVariable UUID id,
                                             @AuthenticationPrincipal Jwt jwt) {
        return meetingService.join(id, actor(jwt), displayName(jwt), email(jwt));
    }

    @PostMapping("/meetings/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        meetingService.leave(id, actor(jwt));
    }

    // -----------------------------------------------------------------------
    // Participants
    // -----------------------------------------------------------------------

    @GetMapping("/meetings/{id}/participants")
    public List<MeetingParticipant> participants(@PathVariable UUID id) {
        return meetingService.participants(id);
    }

    @PostMapping("/meetings/{id}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingParticipant invite(@PathVariable UUID id,
                                     @AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody InviteRequest r) {
        return meetingService.invite(id, r.userId(), r.displayName(), r.email(), actor(jwt));
    }

    @DeleteMapping("/meetings/{id}/participants/{participantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeParticipant(@PathVariable UUID id,
                                   @PathVariable UUID participantId,
                                   @AuthenticationPrincipal Jwt jwt) {
        meetingService.removeParticipant(id, participantId, actor(jwt));
    }

    @PostMapping("/meetings/{id}/participants/{participantId}/promote")
    public MeetingParticipant promote(@PathVariable UUID id,
                                      @PathVariable UUID participantId,
                                      @AuthenticationPrincipal Jwt jwt) {
        return meetingService.promoteToCoHost(id, participantId, actor(jwt));
    }

    // -----------------------------------------------------------------------
    // Chat
    // -----------------------------------------------------------------------

    @PostMapping("/meetings/{id}/chat")
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingChatMessage sendMessage(@PathVariable UUID id,
                                          @AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody ChatMessageRequest r) {
        return meetingService.sendMessage(id, actor(jwt), displayName(jwt), r.content());
    }

    @GetMapping("/meetings/{id}/chat")
    public List<MeetingChatMessage> chatHistory(@PathVariable UUID id) {
        return meetingService.chatHistory(id);
    }

    // -----------------------------------------------------------------------
    // WebRTC Signaling
    // -----------------------------------------------------------------------

    @PostMapping("/meetings/{id}/signal")
    public MeetingSignal sendSignal(@PathVariable UUID id,
                                   @AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody SignalRequest r) {
        return signalService.broadcast(
                id.toString(),
                r.type(),
                r.senderPeerId(),
                r.senderName() != null && !r.senderName().isBlank() ? r.senderName() : displayName(jwt),
                r.targetPeerId(),
                r.payload()
        );
    }

    @GetMapping("/meetings/{id}/signal")
    public List<MeetingSignal> getSignals(@PathVariable UUID id,
                                          @RequestParam(required = false) String peerId,
                                          @RequestParam(defaultValue = "0") long since) {
        return signalService.getSignals(id.toString(), peerId, since);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes());
    }

    private static String displayName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return (name != null && !name.isBlank()) ? name : jwt.getSubject();
    }

    private static String email(Jwt jwt) {
        String emailClaim = jwt.getClaimAsString("email");
        return (emailClaim != null && !emailClaim.isBlank()) ? emailClaim : "";
    }

    // -----------------------------------------------------------------------
    // Request records
    // -----------------------------------------------------------------------

    public record CreateMeetingRequest(
            @NotBlank @Size(max = 300) String title,
            String description,
            @NotNull MeetingType meetingType,
            @Min(2) @Max(100) Integer maxParticipants,
            UUID organizationId,
            UUID projectId,
            Instant scheduledStart,
            Instant scheduledEnd) {}

    public record UpdateMeetingRequest(
            @NotBlank @Size(max = 300) String title,
            String description,
            Instant scheduledStart,
            Instant scheduledEnd) {}

    public record InviteRequest(
            @NotNull UUID userId,
            @NotBlank String displayName,
            String email) {}

    public record ChatMessageRequest(@NotBlank @Size(max = 2000) String content) {}

    public record SignalRequest(
            @NotBlank String type,
            @NotBlank String senderPeerId,
            String senderName,
            String targetPeerId,
            Object payload) {}
}
