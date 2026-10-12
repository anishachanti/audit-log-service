package com.anish.auditlogservice;

import java.time.Instant;
import java.util.List;

public class AuditEventExportBundle {

     private Instant exportedAt;
     private String filterType;
     private String filterValue;
     private String hashAlgorithm;
     private String verificationScope;
     private List<AuditEventEntity> events;

    public AuditEventExportBundle(Instant exportedAt, String filterType, String filterValue, String hashAlgorithm, String verificationScope, List<AuditEventEntity> events) {
        this.exportedAt = exportedAt;
        this.filterType = filterType;
        this.filterValue = filterValue;
        this.hashAlgorithm = hashAlgorithm;
        this.verificationScope = verificationScope;
        this.events = events;
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

    public List<AuditEventEntity> getEvents() {
        return events;
    }
}