package com.anish.auditlogservice;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_persistence_failure_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuditEventPersistenceFailureTests {

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @MockitoSpyBean
    private AuditEventRepository auditEventRepositorySpy;

    @Autowired
    private RedactionRecordRepository redactionRecordRepository;

    @MockitoSpyBean
    private RedactionRecordRepository redactionRecordRepositorySpy;

    @Autowired
    private AuditChainHeadRepository chainHeadRepository;

    @MockitoSpyBean
    private AuditChainHeadRepository chainHeadRepositorySpy;

    @BeforeEach
    void signInAsAdmin() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "persistence-test-admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_AUDIT_ADMIN")));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chainHeadSaveFailureRollsBackTheNewEvent() {
        long eventCountBefore = auditEventRepository.count();
        AuditChainHeadEntity headBefore = chainHeadRepository
                .findById(AuditChainHeadEntity.SINGLETON_ID)
                .orElseThrow();

        long oldSequence = headBefore.getLastSequenceNumber();
        String oldHash = headBefore.getLastHash();

        doThrow(new DataIntegrityViolationException("Simulated database failure"))
                .when(chainHeadRepositorySpy)
                .save(any(AuditChainHeadEntity.class));

        AuditEventRequest request = new AuditEventRequest();
        request.setEventType("ROLLBACK_TEST");
        request.setActorId("test-user");
        request.setResourceType("TEST_RESOURCE");
        request.setResourceId("rollback-test-1");
        request.setPayload(Map.of("example", "test"));

        assertThrows(DataIntegrityViolationException.class,
                () -> auditEventService.createEvent(request));

        assertEquals(eventCountBefore, auditEventRepository.count(),
                "The event should be rolled back when saving the chain head fails");

        AuditChainHeadEntity headAfter = chainHeadRepository
                .findById(AuditChainHeadEntity.SINGLETON_ID)
                .orElseThrow();

        assertEquals(oldSequence, headAfter.getLastSequenceNumber());
        assertEquals(oldHash, headAfter.getLastHash());
    }

    @Test
    void eventSaveFailureLeavesChainHeadUnchanged() {
        long eventCountBefore = auditEventRepository.count();
        AuditChainHeadEntity headBefore = chainHeadRepository
                .findById(AuditChainHeadEntity.SINGLETON_ID)
                .orElseThrow();
        long oldSequence = headBefore.getLastSequenceNumber();
        String oldHash = headBefore.getLastHash();

        doThrow(new DataIntegrityViolationException("Simulated event save failure"))
                .when(auditEventRepositorySpy)
                .save(any(AuditEventEntity.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> auditEventService.createEvent(createRequest()));

        assertEquals(eventCountBefore, auditEventRepository.count());
        AuditChainHeadEntity headAfter = chainHeadRepository
                .findById(AuditChainHeadEntity.SINGLETON_ID)
                .orElseThrow();
        assertEquals(oldSequence, headAfter.getLastSequenceNumber());
        assertEquals(oldHash, headAfter.getLastHash());
    }

    @Test
    void redactionRecordSaveFailureRollsBackPayloadRedaction() {
        AuditEventEntity event = auditEventService.createEvent(createRequest());
        String originalPayload = event.getPayload();

        doThrow(new DataIntegrityViolationException(
                "Simulated redaction record save failure"))
                .when(redactionRecordRepositorySpy)
                .save(any(RedactionRecordEntity.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> auditEventService.redactPayload(
                        event.getSequenceNumber(), List.of("example")));

        AuditEventEntity persistedEvent = auditEventRepository
                .findById(event.getSequenceNumber())
                .orElseThrow();
        assertFalse(persistedEvent.isRedacted());
        assertEquals(originalPayload, persistedEvent.getPayload());
        assertTrue(redactionRecordRepository
                .findByEventSequenceNumber(event.getSequenceNumber())
                .isEmpty());
    }

    @Test
    void verificationReportsMissingChainHead() {
        chainHeadRepository.deleteById(AuditChainHeadEntity.SINGLETON_ID);
        chainHeadRepository.flush();

        try {
            AuditVerificationResponse response = auditEventService.verifyChain();

            assertFalse(response.isIntact());
            assertEquals("CHAIN_HEAD_MISSING", response.getViolationType());
        } finally {
            chainHeadRepository.saveAndFlush(AuditChainHeadEntity.initial());
        }
    }

    @Test
    void verificationReportsChainHeadThatDoesNotMatchEvents() {
        AuditEventEntity event = auditEventService.createEvent(createRequest());
        AuditChainHeadEntity head = chainHeadRepository
                .findById(AuditChainHeadEntity.SINGLETON_ID)
                .orElseThrow();
        Long correctSequence = head.getLastSequenceNumber();
        String correctHash = head.getLastHash();

        head.update(correctSequence, "tampered-chain-head-hash");
        chainHeadRepository.saveAndFlush(head);

        try {
            AuditVerificationResponse response = auditEventService.verifyChain();

            assertFalse(response.isIntact());
            assertEquals("CHAIN_HEAD_MISMATCH", response.getViolationType());
            assertEquals(event.getSequenceNumber(),
                    response.getFirstInvalidSequenceNumber());
        } finally {
            head.update(correctSequence, correctHash);
            chainHeadRepository.saveAndFlush(head);
        }
    }

    private AuditEventRequest createRequest() {
        AuditEventRequest request = new AuditEventRequest();
        request.setEventType("PERSISTENCE_FAILURE_TEST");
        request.setActorId("test-user");
        request.setResourceType("TEST_RESOURCE");
        request.setResourceId("rollback-test-1");
        request.setPayload(Map.of("example", "test"));
        return request;
    }
}
