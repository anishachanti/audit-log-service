
package com.anish.auditlogservice;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.List;
import java.time.temporal.ChronoUnit;

@Service
public class AuditEventService {

    private static final String GENESIS_HASH = "GENESIS";

    private final AuditEventRepository repository;
    private final HashService hashService;
    private final ObjectMapper objectMapper;

    @Value("${audit.retention.days:365}")
    private int retentionDays;

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
         // Map<String, Object> payload,
            String payloadHash,
            Instant timestamp,
            String previousHash) throws JacksonException {

        Map<String, Object> content = new TreeMap<>();
        content.put("id", id.toString());
        content.put("eventType", eventType);
        content.put("actorId", actorId);
        content.put("resourceType", resourceType);
        content.put("resourceId", resourceId);
        content.put("payloadHash", payloadHash);
        content.put("timestamp", timestamp.toString());
        content.put("previousHash", previousHash);

        return objectMapper.writeValueAsString(content);
    }


    private Map<String, Object> normalizePayload(Map<String, Object> payload)
                throws JacksonException {
        String payloadJson = objectMapper.writeValueAsString(payload);

        return objectMapper.readValue(
                payloadJson,
                objectMapper.getTypeFactory()
                        .constructMapType(
                                TreeMap.class,
                                String.class,
                                Object.class
                        )
        );
    }


    private String calculatePayloadHash(Map<String, Object> payload)
        throws JacksonException {
                String canonicalPayload = objectMapper.writeValueAsString(
                normalizePayload(payload)
                );

        return hashService.sha256(canonicalPayload);
        }


    private String calculateStoredEventHash(AuditEventEntity event) {
        try {
                String hashInput = createHashInput(
                        event.getId(),
                        event.getEventType(),
                        event.getActorId(),
                        event.getResourceType(),
                        event.getResourceId(),
                        event.getPayloadHash(),
                        event.getTimestamp(),
                        event.getPreviousHash()
                         );
                        return hashService.sha256(hashInput);
                         } catch (JacksonException e) {
                                throw new IllegalStateException(
                                         "Failed to calculate stored audit event hash", e);
                                         }
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
                objectMapper.writeValueAsString(normalizePayload(request.getPayload()));

        // JsonNode canonicalPayload = objectMapper.readTree(payloadJson);
        String payloadHash = calculatePayloadHash(request.getPayload());

        String hashInput = createHashInput(
                id,
                request.getEventType(),
                request.getActorId(),
                request.getResourceType(),
                request.getResourceId(),
                // normalizePayload(request.getPayload()),
                payloadHash,
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
        event.setPayloadHash(payloadHash);
        event.setTimestamp(timestamp);
        event.setPreviousHash(previousHash);
        event.setHash(hash);

        return repository.save(event);

    } catch (JacksonException e) {
        throw new IllegalStateException(
                "Failed to serialize audit event", e);
    }

    }

    public AuditEventEntity archiveEvent(Long sequenceNumber) {
        AuditEventEntity event = repository.findById(sequenceNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Audit event not found: " + sequenceNumber
                ));

        if (!event.isArchived()) {
                event.setArchived(true);
                event.setArchivedAt(Instant.now());
                return repository.save(event);
        }
        return event;
    }


    public int archiveExpiredEvents() {
        if (retentionDays < 1) {
            throw new IllegalArgumentException(
                    "Retention days must be at least 1"
            );
        }

        Instant cutoff = Instant.now()
            .minus(retentionDays, ChronoUnit.DAYS);

        var events = repository.findAllByOrderBySequenceNumberAsc();

        int archivedCount = 0;

        for (AuditEventEntity event : events) {
            if (!event.isArchived() && event.getTimestamp().isBefore(cutoff)) {
                event.setArchived(true);
                event.setArchivedAt(Instant.now());
                archivedCount++;
            }
        }

        repository.saveAll(events);

        return archivedCount;
    }


//     public AuditEventEntity redactPayload(Long sequenceNumber) {
//         AuditEventEntity event = repository.findById(sequenceNumber)
//                 .orElseThrow(() -> new IllegalArgumentException(
//                         "Audit event not found: " + sequenceNumber
//                 ));

//         if (!event.isRedacted()) {
//                 event.setPayload("[REDACTED]");
//                 event.setRedacted(true);
//                 event.setRedactedAt(Instant.now());

//                 return repository.save(event);
//         }
//         return event;
//     }


        public AuditEventEntity redactPayload(
                Long sequenceNumber,
                List<String> fields) {

        if (fields == null || fields.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one field must be provided for redaction");
        }

        if (fields.stream().anyMatch(
                field -> field == null || field.isBlank())) {
                throw new IllegalArgumentException(
                        "Redaction field names must not be blank");
        }

        AuditEventEntity event = repository.findById(sequenceNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Audit event not found: " + sequenceNumber));

        if (event.isRedacted()) {
                throw new IllegalArgumentException(
                        "Audit event has already been redacted");
        }

        try {
                Map<String, Object> payload = objectMapper.readValue(
                        event.getPayload(),
                        objectMapper.getTypeFactory().constructMapType(
                                TreeMap.class,
                                String.class,
                                Object.class));

                for (String field : fields) {
                if (!payload.containsKey(field)) {
                        throw new IllegalArgumentException(
                                "Payload field not found: " + field);
                }
                }

                for (String field : fields) {
                payload.put(field, "[REDACTED]");
                }

                event.setPayload(objectMapper.writeValueAsString(payload));
                event.setRedacted(true);
                event.setRedactedAt(Instant.now());

                // Preserve payloadHash and hash to maintain the existing chain.
                return repository.save(event);

        } catch (JacksonException exception) {
                throw new IllegalArgumentException(
                        "Stored payload could not be parsed for redaction",
                        exception);
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


    public List<AuditEventEntity> exportEvents(String actorId, String resourceId) {
        boolean hasActorId = actorId != null && !actorId.isBlank();
        boolean hasResourceId = resourceId != null && !resourceId.isBlank();

        if (hasActorId == hasResourceId) {
            throw new IllegalArgumentException(
                    "Exactly one of 'actorId' or 'resourceId' must be provided"
            );
        }

        if (hasActorId) {
                return repository.findAllByActorIdOrderBySequenceNumberAsc(actorId);
        }

        return repository.findAllByResourceIdOrderBySequenceNumberAsc(resourceId);
    }


    public AuditEventExportBundle createExportBundle(
           String actorId,
           String resourceId) {

        List<AuditEventEntity> events = exportEvents(actorId, resourceId);

        boolean hasActorId = actorId != null && !actorId.isBlank();

        return new AuditEventExportBundle(
                Instant.now(),
                hasActorId ? "actorId" : "resourceId",
                hasActorId ? actorId : resourceId,
                "SHA-256",
                "Each event's hash can be recalculated from its metadata and "
                + "payloadHash using the documented SHA-256 canonicalization "
                + "rules. For non-redacted events, payloadHash can also be "
                + "checked against the payload. A filtered export may omit "
                + "predecessor events, so the complete chain cannot be "
                + "verified from this bundle alone. Redacted payloads cannot "
                + "be checked against their original contents.",
                events
        );
  }



    public AuditVerificationResponse verifyChain() {

        var events = repository.findAllByOrderBySequenceNumberAsc();

        String expectedPreviousHash = GENESIS_HASH;
        List<Long> redactedSequenceNumbers = new java.util.ArrayList<>();

        for (AuditEventEntity event : events) {
                System.out.println( "Checking sequence: " + event.getSequenceNumber());

                // Check whether this event links to the previous event.
                if (!expectedPreviousHash.equals(event.getPreviousHash())) {
                        return new AuditVerificationResponse(
                                false,
                                event.getSequenceNumber(),
                                "PREVIOUS_HASH_MISMATCH",
                                "Previous hash does not match the preceding event"
                        );
                }


                if (!event.isRedacted()) {
                        try {
                            Map<String, Object> payload = objectMapper.readValue(
                                    event.getPayload(),
                                    objectMapper.getTypeFactory()
                                            .constructMapType(
                                                    TreeMap.class,
                                                    String.class,
                                                    Object.class
                                            )
                            );

                            String calculatedPayloadHash = calculatePayloadHash(payload);

                            if (!calculatedPayloadHash.equals(event.getPayloadHash())) {
                                return new AuditVerificationResponse(
                                        false,
                                        event.getSequenceNumber(),
                                        "PAYLOAD_HASH_MISMATCH",
                                        "Stored payload does not match its payload hash"
                                );
                            }
                        } catch (JacksonException e) {
                            return new AuditVerificationResponse(
                                    false,
                                    event.getSequenceNumber(),
                                    "INVALID_PAYLOAD",
                                    "Stored payload could not be parsed or verified"
                            );
                        }
                } else {
                        // We can verify the stored commitment in the event hash,
                        // but cannot recalculate it from the removed payload.
                        redactedSequenceNumbers.add(event.getSequenceNumber());
                }

                String calculatedHash = calculateStoredEventHash(event);

                if (!calculatedHash.equals(event.getHash())) {
                        return new AuditVerificationResponse(
                                false,
                                event.getSequenceNumber(),
                                "CONTENT_HASH_MISMATCH",
                                "Event content does not match its stored hash"
                        );
                }

                expectedPreviousHash = event.getHash();


        }

        if (!redactedSequenceNumbers.isEmpty()) {
                return new AuditVerificationResponse(
                        true,
                        null,
                        null,
                        "Audit chain links and stored event hashes verified successfully."
                        + " One or more redacted payloads could not be independently"
                        + " verified against their original payload hashes.",
                        "PARTIAL_REDACTED",
                        redactedSequenceNumbers
                );
        }

        return new AuditVerificationResponse(
                true,
                null,
                null,
                "Audit chain links verified successfully",
                "FULL",
                List.of()
        );
   }



        public ComplianceReport generateComplianceReport(
                Instant from,
                Instant to,
                String actorId,
                String resourceId,
                int page,
                int size) {

        Page<AuditEventEntity> events = queryEvents(
                actorId,
                null,
                resourceId,
                null,
                from,
                to,
                page,
                size
        );

        AuditVerificationResponse verification = verifyChain();

        return new ComplianceReport(
                Instant.now(),
                from,
                to,
                resourceId,
                actorId,
                events.getTotalElements(),
                verification.isIntact(),
                verification.getMessage(),
                events
        );
        }


}

