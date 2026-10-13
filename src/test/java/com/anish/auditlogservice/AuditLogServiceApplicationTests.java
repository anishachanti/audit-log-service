
package com.anish.auditlogservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.TreeMap;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class AuditLogServiceApplicationTests {

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private RedactionRecordRepository redactionRecordRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(auditEventService);
    }

    @Test
    void shouldCreateAuditEvent() {
        AuditEventRequest request = new AuditEventRequest();
        request.setEventType("ACCOUNT_VIEWED");
        request.setActorId("user-test");
        request.setResourceType("ACCOUNT");
        request.setResourceId("account-test");
        request.setPayload(Map.of("action", "view"));

        AuditEventEntity savedEvent = auditEventService.createEvent(request);

        assertNotNull(savedEvent.getSequenceNumber());
        assertNotNull(savedEvent.getId());
        assertNotNull(savedEvent.getTimestamp());
        assertNotNull(savedEvent.getHash());
        assertFalse(savedEvent.getHash().isBlank());
        assertEquals("user-test", savedEvent.getActorId());
        assertEquals("GENESIS", savedEvent.getPreviousHash());
    }

    @Test
    void shouldVerifyAnUntamperedChain() {
        AuditEventRequest request = new AuditEventRequest();
        request.setEventType("ACCOUNT_VIEWED");
        request.setActorId("user-test");
        request.setResourceType("ACCOUNT");
        request.setResourceId("account-test");
        request.setPayload(Map.of("action", "view"));

        auditEventService.createEvent(request);

        AuditVerificationResponse response = auditEventService.verifyChain();

        assertTrue(response.isIntact(), response.getMessage());
        assertNull(response.getFirstInvalidSequenceNumber());
    }

	
@Test
void shouldQueryEventsByActorId() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("query-test-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("account-query-test");
    request.setPayload(Map.of("action", "view"));

    auditEventService.createEvent(request);

    var results = auditEventService.queryEvents(
            "query-test-user",
            null,
            null,
            null,
            null,
            null,
            0,
            10
    );

    assertEquals(1, results.getTotalElements());
    assertEquals(
            "query-test-user",
            results.getContent().get(0).getActorId()
    );
}


@Test
void shouldDetectTamperedEvent() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("tamper-test-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("account-tamper-test");
    request.setPayload(Map.of("action", "view"));

    AuditEventEntity event = auditEventService.createEvent(request);

    // Simulate someone modifying the database directly.
    event.setActorId("unauthorized-user");
    auditEventRepository.saveAndFlush(event);

    AuditVerificationResponse response =
            auditEventService.verifyChain();

    assertFalse(response.isIntact());
    assertEquals("CONTENT_HASH_MISMATCH", response.getViolationType());
    assertEquals(
            event.getSequenceNumber(),
            response.getFirstInvalidSequenceNumber()
    );
}

@Test
void shouldArchiveExpiredEvents() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("retention-test-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("retention-test-account");
    request.setPayload(Map.of("action", "view"));

    AuditEventEntity event = auditEventService.createEvent(request);

    event.setTimestamp(Instant.now().minus(400, ChronoUnit.DAYS));
    auditEventRepository.saveAndFlush(event);

    int archivedCount = auditEventService.archiveExpiredEvents();

    assertEquals(1, archivedCount);

    AuditEventEntity archivedEvent =
            auditEventRepository.findById(event.getSequenceNumber())
                    .orElseThrow();

    assertTrue(archivedEvent.isArchived());
    assertNotNull(archivedEvent.getArchivedAt());
}

@Test
void shouldExportEventsByActorId() {
     AuditEventRequest request = new AuditEventRequest();
     request.setEventType("ACCOUNT_UPDATED");
     request.setActorId("export-test-user");
     request.setResourceType("ACCOUNT");
     request.setResourceId("export-test-account");
     request.setPayload(Map.of("status", "ACTIVE"));

     AuditEventEntity created = auditEventService.createEvent(request);

      var events = auditEventService.exportEvents(
              "export-test-user",
              null
      );

        assertEquals(1, events.size());
        assertEquals(
                created.getSequenceNumber(),
                events.get(0).getSequenceNumber()
        );
        assertEquals(
                "export-test-user",
                events.get(0).getActorId()
        );

    }

    @Test
    void shouldExportEventsByResourceId() {
        AuditEventRequest request = new AuditEventRequest();
        request.setEventType("ACCOUNT_UPDATED");
        request.setActorId("resource-export-user");
        request.setResourceType("ACCOUNT");
        request.setResourceId("resource-export-account");
        request.setPayload(Map.of("status", "ACTIVE"));

        AuditEventEntity created = auditEventService.createEvent(request);

        var events = auditEventService.exportEvents(
                null,
                "resource-export-account"
        );

        assertEquals(1, events.size());
        assertEquals(
                created.getSequenceNumber(),
                events.get(0).getSequenceNumber()
        );
        assertEquals(
                "resource-export-account",
                events.get(0).getResourceId()
        );
    }

    @Test
    void shouldRejectExportWhenBothFiltersAreMissing() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> auditEventService.exportEvents(null, null)
        );

        assertEquals(
                "Exactly one of 'actorId' or 'resourceId' must be provided",
                exception.getMessage()
        );
    }


    @Test
    void shouldIncludeVerificationMetadataInExportBundle() {
        AuditEventRequest request = new AuditEventRequest();
        request.setEventType("ACCOUNT_UPDATED");
        request.setActorId("bundle-test-user");
        request.setResourceType("ACCOUNT");
        request.setResourceId("bundle-test-account");
        request.setPayload(Map.of("status", "ACTIVE"));

        auditEventService.createEvent(request);

        AuditEventExportBundle bundle =
            auditEventService.createExportBundle(
                    "bundle-test-user", null);

        assertEquals("actorId", bundle.getFilterType());
        assertEquals("bundle-test-user", bundle.getFilterValue());
        assertEquals("SHA-256", bundle.getHashAlgorithm());
        assertNotNull(bundle.getExportedAt());
        assertNotNull(bundle.getCanonicalization());
        assertTrue(bundle.getCanonicalization().contains("payloadHash"));
        assertNotNull(bundle.getVerificationLimitations());
        assertTrue(bundle.getVerificationLimitations()
                .contains("predecessor events"));
        assertEquals(1, bundle.getEvents().size());
        }


    
@Test
void shouldPreserveEventHashesInExportBundle() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_UPDATED");
    request.setActorId("hash-export-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("hash-export-account");
    request.setPayload(Map.of("status", "ACTIVE"));

    AuditEventEntity created = auditEventService.createEvent(request);

    AuditEventExportBundle bundle =
            auditEventService.createExportBundle(
                    "hash-export-user", null);

    AuditEventEntity exported = bundle.getEvents().get(0);

    assertEquals(created.getHash(), exported.getHash());
    assertEquals(created.getPayloadHash(), exported.getPayloadHash());
    assertEquals(created.getPreviousHash(), exported.getPreviousHash());
    assertEquals(created.getSequenceNumber(), exported.getSequenceNumber());
}



@Test
void shouldAllowIndependentRecalculationOfExportedEventHash() throws Exception {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_UPDATED");
    request.setActorId("independent-hash-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("independent-hash-account");
    request.setPayload(Map.of("status", "ACTIVE"));

    auditEventService.createEvent(request);

    AuditEventExportBundle bundle =
            auditEventService.createExportBundle(
                    "independent-hash-user", null);

    AuditEventEntity event = bundle.getEvents().get(0);

    Map<String, Object> hashInput = new TreeMap<>();
    hashInput.put("id", event.getId().toString());
    hashInput.put("eventType", event.getEventType());
    hashInput.put("actorId", event.getActorId());
    hashInput.put("resourceType", event.getResourceType());
    hashInput.put("resourceId", event.getResourceId());
    hashInput.put("payloadHash", event.getPayloadHash());
    hashInput.put("timestamp", event.getTimestamp().toString());
    hashInput.put("previousHash", event.getPreviousHash());

    tools.jackson.databind.ObjectMapper mapper =
            new tools.jackson.databind.ObjectMapper();

    String canonicalInput = mapper.writeValueAsString(hashInput);

    String recalculatedHash = new HashService().sha256(canonicalInput);

    assertEquals(event.getHash(), recalculatedHash);
}


@Test
void shouldVerifyExportedPayloadHash() throws Exception {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_UPDATED");
    request.setActorId("payload-hash-test-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("payload-hash-test-account");
    request.setPayload(Map.of(
            "status", "ACTIVE",
            "action", "UPDATE"
    ));

    auditEventService.createEvent(request);

    AuditEventExportBundle bundle =
            auditEventService.createExportBundle(
                    "payload-hash-test-user", null);

    AuditEventEntity exported = bundle.getEvents().get(0);

    assertFalse(exported.isRedacted());

    tools.jackson.databind.ObjectMapper mapper =
            new tools.jackson.databind.ObjectMapper();

    Map<String, Object> canonicalPayload = mapper.readValue(
            exported.getPayload(),
            mapper.getTypeFactory().constructMapType(
                    java.util.TreeMap.class,
                    String.class,
                    Object.class
            )
    );

    String canonicalJson = mapper.writeValueAsString(canonicalPayload);

    String recalculatedPayloadHash =
            new HashService().sha256(canonicalJson);

    assertEquals(exported.getPayloadHash(), recalculatedPayloadHash);
}



@Test
void shouldGenerateComplianceReportForResource() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("compliance-test-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("compliance-test-account");
    request.setPayload(Map.of("action", "view"));

    AuditEventEntity created = auditEventService.createEvent(request);

    ComplianceReport report =
            auditEventService.generateComplianceReport(
                    created.getTimestamp().minusSeconds(60),
                    created.getTimestamp().plusSeconds(60),
                    null,
                    "compliance-test-account",
                    0,
                    10
            );

    assertEquals(1, report.getTotalEvents());
    assertTrue(report.isAuditChainIntact());
    assertNotNull(report.getGeneratedAt());
    assertEquals(
            "compliance-test-account",
            report.getEvents().getContent().get(0).getResourceId()
    );
}



@Test
void shouldReturnEmptyComplianceReportWhenNoEventsMatch() {
    ComplianceReport report =
            auditEventService.generateComplianceReport(
                    Instant.parse("2020-01-01T00:00:00Z"),
                    Instant.parse("2020-01-02T00:00:00Z"),
                    null,
                    "nonexistent-compliance-account",
                    0,
                    10
            );

    assertEquals(0, report.getTotalEvents());
    assertTrue(report.getEvents().isEmpty());
    assertTrue(report.isAuditChainIntact());
    assertNotNull(report.getGeneratedAt());
}



@Test
void shouldIncludeAuditChainVerificationStatusInComplianceReport() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("verification-report-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("verification-report-account");
    request.setPayload(Map.of("action", "view"));

    auditEventService.createEvent(request);

    ComplianceReport report =
            auditEventService.generateComplianceReport(
                    null,
                    null,
                    "verification-report-user",
                    null,
                    0,
                    10
            );

    assertTrue(report.isAuditChainIntact());
    assertEquals(
            "Audit chain links verified successfully",
            report.getVerificationMessage()
    );
    assertEquals(1, report.getTotalEvents());
}



@Test
void shouldRedactSelectedPayloadFieldsAndPreserveHashChain() throws Exception {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("redaction-test-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("redaction-test-account");
    request.setPayload(Map.of(
            "accountNumber", "123456789",
            "email", "user@example.com",
            "action", "VIEW"
    ));

    AuditEventEntity created = auditEventService.createEvent(request);
    String originalHash = created.getHash();
    String originalPayloadHash = created.getPayloadHash();

    AuditEventEntity redacted = auditEventService.redactPayload(
            created.getSequenceNumber(),
            java.util.List.of("accountNumber", "email")
    );

    tools.jackson.databind.ObjectMapper mapper =
            new tools.jackson.databind.ObjectMapper();

    Map<String, Object> payload = mapper.readValue(
            redacted.getPayload(),
            mapper.getTypeFactory().constructMapType(
                    TreeMap.class,
                    String.class,
                    Object.class
            )
    );

    assertEquals("[REDACTED]", payload.get("accountNumber"));
    assertEquals("[REDACTED]", payload.get("email"));
    assertEquals("VIEW", payload.get("action"));
    assertEquals(originalHash, redacted.getHash());
    assertEquals(originalPayloadHash, redacted.getPayloadHash());
    assertTrue(redacted.isRedacted());

    AuditVerificationResponse verification =
            auditEventService.verifyChain();

    assertTrue(verification.isIntact(), verification.getMessage());
}


@Test
void shouldReportRedactionLimitationDuringChainVerification() throws Exception {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("redaction-verification-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("redaction-verification-account");
    request.setPayload(Map.of(
            "accountNumber", "123456789",
            "action", "VIEW"
    ));

    AuditEventEntity created = auditEventService.createEvent(request);

    auditEventService.redactPayload(
            created.getSequenceNumber(),
            java.util.List.of("accountNumber")
    );

    AuditVerificationResponse response = auditEventService.verifyChain();

    assertTrue(response.isIntact(), response.getMessage());
    assertNull(response.getFirstInvalidSequenceNumber());
    assertNull(response.getViolationType());
    assertTrue(
            response.getMessage().contains("redacted payloads"),
            response.getMessage()
    );
    assertTrue(
            response.getMessage().contains("original payload hashes"),
            response.getMessage()
    );
}


@Test
void shouldDetectTamperingWithRedactedPayload() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("redaction-tampering-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("redaction-tampering-account");
    request.setPayload(Map.of(
            "accountNumber", "123456789",
            "action", "VIEW"
    ));

    AuditEventEntity created = auditEventService.createEvent(request);

    AuditEventEntity redacted = auditEventService.redactPayload(
            created.getSequenceNumber(),
            java.util.List.of("accountNumber")
    );

    // Simulate an unauthorized change to the stored payload.
    redacted.setPayload(
            "{\"accountNumber\":\"ATTACKER-CHANGED\",\"action\":\"VIEW\"}");
    auditEventRepository.saveAndFlush(redacted);

    AuditVerificationResponse response = auditEventService.verifyChain();

    assertFalse(response.isIntact());
    assertEquals("REDACTION_PROOF_INVALID", response.getViolationType());
    assertEquals(
            created.getSequenceNumber(),
            response.getFirstInvalidSequenceNumber()
    );
}

@Test
void shouldRejectRedactedEventWhenProofRecordIsMissing() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("missing-proof-test-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("missing-proof-test-account");
    request.setPayload(Map.of(
            "accountNumber", "123456789",
            "action", "VIEW"
    ));

    AuditEventEntity created = auditEventService.createEvent(request);

    auditEventService.redactPayload(
            created.getSequenceNumber(),
            java.util.List.of("accountNumber")
    );

    RedactionRecordEntity record = redactionRecordRepository
            .findByEventSequenceNumber(created.getSequenceNumber())
            .orElseThrow();

    redactionRecordRepository.delete(record);
    redactionRecordRepository.flush();

    AuditVerificationResponse response = auditEventService.verifyChain();

    assertFalse(response.isIntact());
    assertEquals("REDACTION_PROOF_INVALID", response.getViolationType());
    assertEquals(
            created.getSequenceNumber(),
            response.getFirstInvalidSequenceNumber()
    );
}


@Test
void shouldRejectUnknownRedactionField() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("redaction-validation-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("redaction-validation-account");
    request.setPayload(Map.of("action", "VIEW"));

    AuditEventEntity created = auditEventService.createEvent(request);

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> auditEventService.redactPayload(
                    created.getSequenceNumber(),
                    java.util.List.of("unknownField"))
    );

    assertEquals(
            "Payload field not found: unknownField",
            exception.getMessage()
    );
}



@Test
void shouldRejectEmptyRedactionFieldList() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("empty-redaction-test-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("empty-redaction-test-account");
    request.setPayload(Map.of("action", "VIEW"));

    AuditEventEntity created = auditEventService.createEvent(request);

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> auditEventService.redactPayload(
                    created.getSequenceNumber(),
                    java.util.List.of())
    );

    assertEquals(
            "At least one field must be provided for redaction",
            exception.getMessage()
    );
}

@Test
void shouldRejectRedactionForNonexistentEvent() {
    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> auditEventService.redactPayload(
                    Long.MAX_VALUE,
                    java.util.List.of("accountNumber"))
    );

    assertEquals(
            "Audit event not found: " + Long.MAX_VALUE,
            exception.getMessage()
    );
}

@Test
void shouldRejectRedactingAnAlreadyRedactedEvent() {
    AuditEventRequest request = new AuditEventRequest();
    request.setEventType("ACCOUNT_VIEWED");
    request.setActorId("repeat-redaction-test-user");
    request.setResourceType("ACCOUNT");
    request.setResourceId("repeat-redaction-test-account");
    request.setPayload(Map.of(
            "accountNumber", "123456789",
            "action", "VIEW"
    ));

    AuditEventEntity created = auditEventService.createEvent(request);

    auditEventService.redactPayload(
            created.getSequenceNumber(),
            java.util.List.of("accountNumber")
    );

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> auditEventService.redactPayload(
                    created.getSequenceNumber(),
                    java.util.List.of("action"))
    );

    assertEquals(
            "Audit event has already been redacted",
            exception.getMessage()
    );
}

@Test
void readerCanViewEventsButCannotCreateThem() throws Exception {
    mockMvc.perform(get("/audit/events")
                    .with(httpBasic(
                            "audit-test-reader",
                            "test-only-reader-password")))
            .andExpect(status().isOk());

    mockMvc.perform(post("/audit/events")
                    .with(httpBasic(
                            "audit-test-reader",
                            "test-only-reader-password"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isForbidden());
}


@Test
void adminCanCreateEvent() throws Exception {
    mockMvc.perform(post("/audit/events")
                    .with(httpBasic(
                            "audit-test-admin",
                            "test-only-admin-password"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "eventType": "ACCOUNT_VIEWED",
                              "actorId": "security-test-admin",
                              "resourceType": "ACCOUNT",
                              "resourceId": "security-test-1",
                              "payload": {"action": "view"}
                            }
                            """))
            .andExpect(status().isCreated());
}



}
