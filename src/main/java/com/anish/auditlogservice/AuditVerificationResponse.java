
package com.anish.auditlogservice;

public class AuditVerificationResponse {

    private boolean intact;
    private Long firstInvalidSequenceNumber;
    private String violationType;
    private String message;

    public AuditVerificationResponse() {
    }

    public AuditVerificationResponse(
            boolean intact,
            Long firstInvalidSequenceNumber,
            String violationType,
            String message) {
        this.intact = intact;
        this.firstInvalidSequenceNumber = firstInvalidSequenceNumber;
        this.violationType = violationType;
        this.message = message;
    }

    public boolean isIntact() {
        return intact;
    }

    public void setIntact(boolean intact) {
        this.intact = intact;
    }

    public Long getFirstInvalidSequenceNumber() {
        return firstInvalidSequenceNumber;
    }

    public void setFirstInvalidSequenceNumber(
            Long firstInvalidSequenceNumber) {
        this.firstInvalidSequenceNumber = firstInvalidSequenceNumber;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
