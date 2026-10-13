package com.anish.auditlogservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RedactionRecordRepository
        extends JpaRepository<RedactionRecordEntity, Long> {

    Optional<RedactionRecordEntity> findByEventSequenceNumber(
            Long eventSequenceNumber);
}