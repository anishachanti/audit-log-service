
package com.anish.auditlogservice;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
// import tools.jackson.databind.JsonNode;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class AuditEventService {

    private static final String GENESIS_HASH = "GENESIS";

    private final AuditEventRepository repository;
    private final HashService hashService;
    private final ObjectMapper objectMapper;

    public AuditEventService(
            AuditEventRepository repository,
            HashService hashService,
            ObjectMapper objectMapper) {

        this.repository = repository;
        this.hashService = hashService;
        this.objectMapper = objectMapper;
    }

    private String createHashInput(
            UUID id,
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            Map<String, Object> payload,
            Instant timestamp,
            String previousHash) throws JacksonException {

        Map<String, Object> content = new TreeMap<>();
        content.put("id", id.toString());
        content.put("eventType", eventType);
        content.put("actorId", actorId);
        content.put("resourceType", resourceType);
        content.put("resourceId", resourceId);
        content.put("payload", payload);
        content.put("timestamp", timestamp.toString());
        content.put("previousHash", previousHash);

        return objectMapper.writeValueAsString(content);
    }

    public AuditEventEntity createEvent(AuditEventRequest request) {
        UUID id = UUID.randomUUID();
        // Instant timestamp = Instant.now();
        Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MICROS);

        AuditEventEntity previousEvent =
                repository.findTopByOrderBySequenceNumberDesc()
                        .orElse(null);

        String previousHash = previousEvent == null
                ? GENESIS_HASH
                : previousEvent.getHash();

        
    try {
        String payloadJson =
                objectMapper.writeValueAsString(request.getPayload());

        // JsonNode canonicalPayload = objectMapper.readTree(payloadJson);

        String hashInput = createHashInput(
                id,
                request.getEventType(),
                request.getActorId(),
                request.getResourceType(),
                request.getResourceId(),
                request.getPayload(),
                timestamp,
                previousHash
        );

        String hash = hashService.sha256(hashInput);

        AuditEventEntity event = new AuditEventEntity();
        event.setId(id);
        event.setEventType(request.getEventType());
        event.setActorId(request.getActorId());
        event.setResourceType(request.getResourceType());
        event.setResourceId(request.getResourceId());
        event.setPayload(payloadJson);
        event.setTimestamp(timestamp);
        event.setPreviousHash(previousHash);
        event.setHash(hash);

        return repository.save(event);

    } catch (JacksonException e) {
        throw new IllegalStateException(
                "Failed to serialize audit event", e);
    }

    }
    public Page<AuditEventEntity> queryEvents(
            String actorId,
            String resourceType,
            String resourceId,
            String eventType,
            Instant from,
            Instant to,
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be zero or greater");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "'from' must be earlier than or equal to 'to'");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "sequenceNumber")
        );

        var specification = AuditEventSpecifications.hasActorId(actorId)
                .and(AuditEventSpecifications.hasResourceType(resourceType))
                .and(AuditEventSpecifications.hasResourceId(resourceId))
                .and(AuditEventSpecifications.hasEventType(eventType))
                .and(AuditEventSpecifications.timestampFrom(from))
                .and(AuditEventSpecifications.timestampTo(to));

        return repository.findAll(specification, pageable);
    }


    public AuditVerificationResponse verifyChain() {

        var events = repository.findAllByOrderBySequenceNumberAsc();

        String expectedPreviousHash = GENESIS_HASH;

        for (AuditEventEntity event : events) {

                // Check whether this event links to the previous event.
                if (!expectedPreviousHash.equals(event.getPreviousHash())) {
                return new AuditVerificationResponse(
                        false,
                        event.getSequenceNumber(),
                        "PREVIOUS_HASH_MISMATCH",
                        "Previous hash does not match the preceding event"
                );
                }

                // Move forward using this event's stored hash.
                expectedPreviousHash = event.getHash();
        }

        return new AuditVerificationResponse(
                true,
                null,
                null,
                "Audit chain links verified successfully"
        );
   }

}

