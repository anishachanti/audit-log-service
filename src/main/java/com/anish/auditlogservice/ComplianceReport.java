
package com.anish.auditlogservice;

import java.time.Instant;
import org.springframework.data.domain.Page;

public class ComplianceReport {

    private Instant generatedAt;
    private Instant from;
    private Instant to;
    private String resourceId;
    private String actorId;
    private long totalEvents;
    private boolean auditChainIntact;
    private String verificationMessage;
    private Page<AuditEventEntity> events;

    public ComplianceReport(
            Instant generatedAt,
            Instant from,
            Instant to,
            String resourceId,
            String actorId,
            long totalEvents,
            boolean auditChainIntact,
            String verificationMessage,
            Page<AuditEventEntity> events) {

        this.generatedAt = generatedAt;
        this.from = from;
        this.to = to;
        this.resourceId = resourceId;
        this.actorId = actorId;
        this.totalEvents = totalEvents;
        this.auditChainIntact = auditChainIntact;
        this.verificationMessage = verificationMessage;
        this.events = events;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getActorId() {
        return actorId;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public boolean isAuditChainIntact() {
        return auditChainIntact;
    }

    public String getVerificationMessage() {
        return verificationMessage;
    }

    public Page<AuditEventEntity> getEvents() {
        return events;
    }
}
