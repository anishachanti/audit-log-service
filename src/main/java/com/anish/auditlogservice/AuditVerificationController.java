package com.anish.auditlogservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditVerificationController {

    private final AuditEventService auditEventService;

    public AuditVerificationController(
            AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping("/verify")
    public AuditVerificationResponse verifyChain() {
        return auditEventService.verifyChain();
    }
}