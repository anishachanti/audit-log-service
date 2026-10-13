
package com.anish.auditlogservice;

import java.util.List;

public class AuditVerificationResponse {

    private boolean intact;
    private Long firstInvalidSequenceNumber;
    private String violationType;
    private String message;
    private String payloadVerificationStatus;
    private List<Long> redactedSequenceNumbers = List.of();

    public AuditVerificationResponse() {
    }

    public AuditVerificationResponse(
            boolean intact,
            Long firstInvalidSequenceNumber,
            String violationType,
            String message) {
        this(intact, firstInvalidSequenceNumber, violationType, message,
                intact ? "FULL" : "NOT_COMPLETED", List.of());
    }

    public AuditVerificationResponse(
            boolean intact,
            Long firstInvalidSequenceNumber,
            String violationType,
            String message,
            String payloadVerificationStatus,
            List<Long> redactedSequenceNumbers) {
        this.intact = intact;
        this.firstInvalidSequenceNumber = firstInvalidSequenceNumber;
        this.violationType = violationType;
        this.message = message;
        this.payloadVerificationStatus = payloadVerificationStatus;
        this.redactedSequenceNumbers = List.copyOf(redactedSequenceNumbers);
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

    public String getPayloadVerificationStatus() {
        return payloadVerificationStatus;
    }

    public void setPayloadVerificationStatus(String payloadVerificationStatus) {
        this.payloadVerificationStatus = payloadVerificationStatus;
    }

    public List<Long> getRedactedSequenceNumbers() {
        return redactedSequenceNumbers;
    }

    public void setRedactedSequenceNumbers(List<Long> redactedSequenceNumbers) {
        this.redactedSequenceNumbers = List.copyOf(redactedSequenceNumbers);
    }
}
