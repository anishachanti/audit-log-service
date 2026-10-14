package com.anish.auditlogservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_concurrency_test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuditEventConcurrencyTests {

    private static final int CONCURRENT_WRITERS = 8;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditChainHeadRepository chainHeadRepository;

    @Test
    void concurrentCreatesRemainChained() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_WRITERS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_WRITERS);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<?>> writes = new ArrayList<>();
            for (int writer = 0; writer < CONCURRENT_WRITERS; writer++) {
                int writerNumber = writer;
                writes.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for the concurrent start signal");
                    }
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(new UsernamePasswordAuthenticationToken(
                            "concurrent-test-admin",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_AUDIT_ADMIN"))));
                    SecurityContextHolder.setContext(context);

                    try {
                        auditEventService.createEvent(createRequest(writerNumber));
                        return null;
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS),
                    "All writer threads should be ready before the test starts them");
            start.countDown();

            for (Future<?> write : writes) {
                write.get(30, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        List<AuditEventEntity> events = auditEventRepository.findAllByOrderBySequenceNumberAsc();
        assertEquals(CONCURRENT_WRITERS, events.size(), "Every concurrent create should be stored");

        String expectedPreviousHash = "GENESIS";
        for (int index = 0; index < events.size(); index++) {
            AuditEventEntity event = events.get(index);
            assertEquals(index + 1L, event.getSequenceNumber(),
                    "Sequence numbers should be contiguous");
            assertEquals(expectedPreviousHash, event.getPreviousHash(),
                    "Each event should point to the previous event's hash");
            assertNotNull(event.getHash());
            expectedPreviousHash = event.getHash();
        }

        AuditChainHeadEntity chainHead = chainHeadRepository
                .findById(AuditChainHeadEntity.SINGLETON_ID)
                .orElseThrow();
        assertEquals((long) CONCURRENT_WRITERS, chainHead.getLastSequenceNumber());
        assertEquals(expectedPreviousHash, chainHead.getLastHash());

        SecurityContext verificationContext =
                SecurityContextHolder.createEmptyContext();
        verificationContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "concurrent-test-admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_AUDIT_ADMIN"))));
        SecurityContextHolder.setContext(verificationContext);
        try {
            AuditVerificationResponse verification = auditEventService.verifyChain();
            assertTrue(verification.isIntact(), verification.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private AuditEventRequest createRequest(int writerNumber) {
        AuditEventRequest request = new AuditEventRequest();
        request.setEventType("CONCURRENT_TEST_EVENT");
        request.setActorId("concurrent-test-writer-" + writerNumber);
        request.setResourceType("TEST_RESOURCE");
        request.setResourceId("concurrent-test-resource-" + writerNumber);
        request.setPayload(Map.of("writer", writerNumber));
        return request;
    }
}
