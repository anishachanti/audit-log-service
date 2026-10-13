package com.anish.auditlogservice;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuditChainHeadRepository
        extends JpaRepository<AuditChainHeadEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select head from AuditChainHeadEntity head where head.id = :id")
    Optional<AuditChainHeadEntity> findByIdForUpdate(@Param("id") Long id);
}