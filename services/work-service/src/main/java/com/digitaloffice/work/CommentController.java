package com.digitaloffice.work;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
public class CommentController {
    private final CommentService service;
    public CommentController(CommentService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TaskComment add(@PathVariable UUID taskId, @AuthenticationPrincipal Jwt jwt,
                            @RequestHeader("Authorization") String token, @Valid @RequestBody CommentRequest request) {
        return service.addComment(taskId, request.content(), actor(jwt), token);
    }
    @GetMapping
    public List<TaskComment> list(@PathVariable UUID taskId, @AuthenticationPrincipal Jwt jwt,
                                  @RequestHeader("Authorization") String token) {
        return service.getComments(taskId, actor(jwt), token);
    }
    private static UUID actor(Jwt jwt) {
        return UUID.nameUUIDFromBytes(("oidc:" + jwt.getSubject()).getBytes(StandardCharsets.UTF_8));
    }
    public record CommentRequest(@NotNull @Size(max = 10001) String content) {}
}
