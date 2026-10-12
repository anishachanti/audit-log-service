
package com.anish.auditlogservice;

import java.time.Instant;
import java.util.List;

public class AuditEventExportBundle {

    private Instant exportedAt;
    private String filterType;
    private String filterValue;
    private String hashAlgorithm;
    private String verificationScope;
    private String canonicalization;
    private String verificationLimitations;
    private List<AuditEventEntity> events;

    public AuditEventExportBundle(
            Instant exportedAt,
            String filterType,
            String filterValue,
            String hashAlgorithm,
            String verificationScope,
            List<AuditEventEntity> events) {

        this.exportedAt = exportedAt;
        this.filterType = filterType;
        this.filterValue = filterValue;
        this.hashAlgorithm = hashAlgorithm;
        this.verificationScope = verificationScope;
        this.events = events;

        this.canonicalization =
                "Payload and event hash inputs are serialized as JSON "
                + "with keys sorted lexicographically using TreeMap. "
                + "The event hash input includes id, eventType, actorId, "
                + "resourceType, resourceId, payloadHash, timestamp, "
                + "and previousHash.";

        this.verificationLimitations =
                "Filtered exports may omit predecessor events, preventing "
                + "full-chain verification. Redacted payloads cannot be "
                + "compared against their original contents. SHA-256 hashes "
                + "alone do not authenticate the exporter.";
    }

    public Instant getExportedAt() {
        return exportedAt;
    }

    public String getFilterType() {
        return filterType;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public String getVerificationScope() {
        return verificationScope;
    }

    public String getCanonicalization() {
        return canonicalization;
    }

    public String getVerificationLimitations() {
        return verificationLimitations;
    }

    public List<AuditEventEntity> getEvents() {
        return events;
    }
}
