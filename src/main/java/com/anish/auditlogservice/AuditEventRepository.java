package com.anish.auditlogservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.List;

public interface AuditEventRepository
        extends JpaRepository<AuditEventEntity, Long>, JpaSpecificationExecutor<AuditEventEntity> {

    Optional<AuditEventEntity> findTopByOrderBySequenceNumberDesc();

    Page<AuditEventEntity> findByActorId(String actorId, Pageable pageable);

    Page<AuditEventEntity> findByResourceTypeAndResourceId(String resourceType, String resourceId, Pageable pageable);

    Page<AuditEventEntity> findByEventType(String eventType, Pageable pageable);

    List<AuditEventEntity> findAllByOrderBySequenceNumberAsc();
    
}

    