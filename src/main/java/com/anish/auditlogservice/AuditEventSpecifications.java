
package com.anish.auditlogservice;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class AuditEventSpecifications {

    private AuditEventSpecifications() {
    }

    public static Specification<AuditEventEntity> hasActorId(
            String actorId) {
        return (root, query, cb) ->
                actorId == null ? cb.conjunction()
                        : cb.equal(root.get("actorId"), actorId);
    }

    public static Specification<AuditEventEntity> hasResourceType(
            String resourceType) {
        return (root, query, cb) ->
                resourceType == null ? cb.conjunction()
                        : cb.equal(root.get("resourceType"), resourceType);
    }

    public static Specification<AuditEventEntity> hasResourceId(
            String resourceId) {
        return (root, query, cb) ->
                resourceId == null ? cb.conjunction()
                        : cb.equal(root.get("resourceId"), resourceId);
    }

    public static Specification<AuditEventEntity> hasEventType(
            String eventType) {
        return (root, query, cb) ->
                eventType == null ? cb.conjunction()
                        : cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<AuditEventEntity> timestampFrom(
            Instant from) {
        return (root, query, cb) ->
                from == null ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                                root.get("timestamp"), from);
    }

    public static Specification<AuditEventEntity> timestampTo(
            Instant to) {
        return (root, query, cb) ->
                to == null ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                                root.get("timestamp"), to);
    }
}
