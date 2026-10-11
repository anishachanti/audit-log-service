
package com.anish.auditlogservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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

}
