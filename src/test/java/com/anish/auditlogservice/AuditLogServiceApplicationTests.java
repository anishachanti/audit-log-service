
package com.anish.auditlogservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuditLogServiceApplicationTests {

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventRepository auditEventRepository;

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


}
