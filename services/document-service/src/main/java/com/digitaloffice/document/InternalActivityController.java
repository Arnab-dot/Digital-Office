package com.digitaloffice.document;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/activity")
public class InternalActivityController {
    private final DocumentActivityEventRepository events;
    public InternalActivityController(DocumentActivityEventRepository events) { this.events = events; }
    @GetMapping
    public List<DocumentActivityEvent> byProject(@RequestParam UUID projectId) {
        return events.findByProjectIdOrderByEventAtDesc(projectId);
    }
}
