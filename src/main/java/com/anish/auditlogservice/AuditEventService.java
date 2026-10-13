
package com.anish.auditlogservice;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

@Service
public class AuditEventService {

    private static final String GENESIS_HASH = "GENESIS";

    private final AuditEventRepository repository;
    private final HashService hashService;
    private final ObjectMapper objectMapper;
    private final RedactionRecordRepository redactionRecordRepository;
    private final RedactionSignatureService redactionSignatureService;
    private final AuditChainHeadRepository chainHeadRepository;

    @Value("${audit.retention.days:365}")
    private int retentionDays;

    public AuditEventService(
            AuditEventRepository repository,
            HashService hashService,
            ObjectMapper objectMapper,
            RedactionRecordRepository redactionRecordRepository,
            RedactionSignatureService redactionSignatureService,
            AuditChainHeadRepository chainHeadRepository) {

        this.repository = repository;
        this.hashService = hashService;
        this.objectMapper = objectMapper;
        this.redactionRecordRepository = redactionRecordRepository;
        this.redactionSignatureService = redactionSignatureService;
        this.chainHeadRepository = chainHeadRepository;
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

    
    private boolean verifyRedactionRecord(AuditEventEntity event) {
        var recordOptional = redactionRecordRepository
                .findByEventSequenceNumber(event.getSequenceNumber());

        if (recordOptional.isEmpty()) {
                return false;
        }

        RedactionRecordEntity record = recordOptional.get();

        if (!event.getSequenceNumber().equals(record.getEventSequenceNumber())
                || !event.getPayloadHash().equals(record.getOriginalPayloadHash())
                || !event.getRedactedAt().equals(record.getRedactedAt())) {
                return false;
        }

        try {
                Map<String, Object> payload = objectMapper.readValue(
                        event.getPayload(),
                        objectMapper.getTypeFactory().constructMapType(
                                TreeMap.class,
                                String.class,
                                Object.class));

                List<String> redactedFields = objectMapper.readValue(
                        record.getRedactedFieldsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class,
                                String.class));

                String actualRedactedPayloadHash = calculatePayloadHash(payload);

                if (!actualRedactedPayloadHash.equals(record.getRedactedPayloadHash())) {
                return false;
                }

                String canonicalRecord = redactionSignatureService.canonicalize(
                        record.getEventSequenceNumber(),
                        record.getOriginalPayloadHash(),
                        record.getRedactedPayloadHash(),
                        redactedFields,
                        record.getRedactedAt());

                return redactionSignatureService.verify(
                        canonicalRecord,
                        record.getSignature());

        } catch (JacksonException exception) {
                return false;
        }
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

    @Transactional
    public AuditEventEntity createEvent(AuditEventRequest request) {
        UUID id = UUID.randomUUID();
        // Instant timestamp = Instant.now();
        Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MICROS);

        AuditChainHeadEntity chainHead = chainHeadRepository
        .findByIdForUpdate(AuditChainHeadEntity.SINGLETON_ID)
        .orElseThrow(() -> new IllegalStateException(
                "Audit chain head has not been initialized"));

        String previousHash = chainHead.getLastHash();        


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

        AuditEventEntity savedEvent = repository.save(event);

        chainHead.update(
                savedEvent.getSequenceNumber(),
                savedEvent.getHash()
        );
        chainHeadRepository.save(chainHead);

        return savedEvent;

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


        @Transactional
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

                Instant redactedAt = Instant.now();
                List<String> canonicalFields = fields.stream()
                        .distinct()
                        .sorted()
                        .toList();

                String originalPayloadHash = event.getPayloadHash();
                String redactedPayloadHash = calculatePayloadHash(payload);
                String redactedFieldsJson = objectMapper.writeValueAsString(canonicalFields);
                
                String canonicalRecord = redactionSignatureService.canonicalize(
                        event.getSequenceNumber(),
                        originalPayloadHash,
                        redactedPayloadHash,
                        canonicalFields,
                        redactedAt
                );
                String signature = redactionSignatureService.sign(canonicalRecord);

                RedactionRecordEntity redactionRecord = new RedactionRecordEntity(
                        event.getSequenceNumber(),
                        originalPayloadHash,
                        redactedPayloadHash,
                        redactedFieldsJson,
                        redactedAt,
                        signature
                );

                event.setPayload(objectMapper.writeValueAsString(payload));
                event.setRedacted(true);
                event.setRedactedAt(redactedAt);

                // Preserve payloadHash and hash to maintain the existing chain.
                redactionRecordRepository.save(redactionRecord);
                return repository.save(event);

        } catch (JacksonException exception) {
                throw new IllegalArgumentException(
                        "Stored payload could not be parsed for redaction",
                        exception);
        }
    }



    private Set<String> getAllowedResourceIdsForCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_AUDIT_ADMIN".equals(authority.getAuthority()));

        if (isAdmin) {
            return null; // Admin has no resource restriction.
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("RESOURCE_ID:"))
                .map(authority -> authority.substring("RESOURCE_ID:".length()))
                .collect(Collectors.toSet());
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

        Set<String> allowedResourceIds = getAllowedResourceIdsForCurrentUser();

        if (allowedResourceIds != null
                && resourceId != null
                && !allowedResourceIds.contains(resourceId)) {
            throw new AccessDeniedException("You cannot access this resource");
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
                .and(AuditEventSpecifications.timestampTo(to))
                .and(AuditEventSpecifications.hasResourceIdIn(allowedResourceIds));

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

        Set<String> allowedResourceIds = getAllowedResourceIdsForCurrentUser();

        if (allowedResourceIds != null
                && hasResourceId
                && !allowedResourceIds.contains(resourceId)) {
            throw new AccessDeniedException("You cannot access this resource");
        }

        var scope = AuditEventSpecifications.hasResourceIdIn(allowedResourceIds);
        var specification = hasActorId
                ? AuditEventSpecifications.hasActorId(actorId).and(scope)
                : AuditEventSpecifications.hasResourceId(resourceId).and(scope);

        return repository.findAll(specification, Sort.by(Sort.Direction.ASC, "sequenceNumber"));
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

                        if (!verifyRedactionRecord(event)) {
                                return new AuditVerificationResponse(
                                        false,
                                        event.getSequenceNumber(),
                                        "REDACTION_PROOF_INVALID",
                                        "Redacted record is missing or its signature or payload commitment is invalid"
                                );
                        }
                        
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

