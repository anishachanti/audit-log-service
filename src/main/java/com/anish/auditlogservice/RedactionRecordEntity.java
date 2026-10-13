package com.anish.auditlogservice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_redactions")
public class RedactionRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One redaction record per event in the current design.
    @Column(nullable = false, unique = true)
    private Long eventSequenceNumber;

    // Commitment to the payload before it was redacted.
    @Column(nullable = false, length = 64)
    private String originalPayloadHash;

    // Hash of the payload after redaction.
    @Column(nullable = false, length = 64)
    private String redactedPayloadHash;

    // JSON array of selected top-level field names.
    @Lob
    @Column(nullable = false)
    private String redactedFieldsJson;

    @Column(nullable = false)
    private Instant redactedAt;

    // HMAC signature will be added by a separate signing service.
    @Column(nullable = false, length = 64)
    private String signature;

    // JPA requires a no-argument constructor.
    protected RedactionRecordEntity() {
    }

    public RedactionRecordEntity(
            Long eventSequenceNumber,
            String originalPayloadHash,
            String redactedPayloadHash,
            String redactedFieldsJson,
            Instant redactedAt,
            String signature) {
        this.eventSequenceNumber = eventSequenceNumber;
        this.originalPayloadHash = originalPayloadHash;
        this.redactedPayloadHash = redactedPayloadHash;
        this.redactedFieldsJson = redactedFieldsJson;
        this.redactedAt = redactedAt;
        this.signature = signature;
    }

    public Long getId() {
        return id;
    }

    public Long getEventSequenceNumber() {
        return eventSequenceNumber;
    }

    public String getOriginalPayloadHash() {
        return originalPayloadHash;
    }

    public String getRedactedPayloadHash() {
        return redactedPayloadHash;
    }

    public String getRedactedFieldsJson() {
        return redactedFieldsJson;
    }

    public Instant getRedactedAt() {
        return redactedAt;
    }

    public String getSignature() {
        return signature;
    }
}