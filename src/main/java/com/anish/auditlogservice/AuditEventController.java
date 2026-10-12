
package com.anish.auditlogservice;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/audit/events")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEventEntity createEvent(
            @Valid @RequestBody AuditEventRequest request) {

        return auditEventService.createEvent(request);
    }

    
    @GetMapping
    public Page<AuditEventEntity> queryEvents(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return auditEventService.queryEvents(
                actorId, resourceType, resourceId,
                eventType, from, to, page, size);
    }

    @PatchMapping("/{sequenceNumber}/archive")
    public AuditEventEntity archiveEvent(@PathVariable Long sequenceNumber) {
        return auditEventService.archiveEvent(sequenceNumber);
    }

    @PostMapping("/retention/archive-expired")
    public Map<String, Object> archiveExpiredEvents() {
        int archivedCount = auditEventService.archiveExpiredEvents();

        return Map.of(
                "archivedCount", archivedCount,
                "message", "Expired audit events archived successfully"
        );
    }

    @PatchMapping("/{sequenceNumber}/redact")
    public AuditEventEntity redactPayload(
            @PathVariable Long sequenceNumber) {
        return auditEventService.redactPayload(sequenceNumber);
    }

}
