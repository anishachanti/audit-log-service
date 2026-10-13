package com.anish.auditlogservice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_chain_head")
public class AuditChainHeadEntity {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false)
    private Long lastSequenceNumber;

    @Column(nullable = false, length = 64)
    private String lastHash;

    protected AuditChainHeadEntity() {
        // Required by JPA
    }

    public AuditChainHeadEntity(
            Long id,
            Long lastSequenceNumber,
            String lastHash) {
        this.id = id;
        this.lastSequenceNumber = lastSequenceNumber;
        this.lastHash = lastHash;
    }

    public static AuditChainHeadEntity initial() {
        return new AuditChainHeadEntity(SINGLETON_ID, 0L, "GENESIS");
    }

    public Long getId() {
        return id;
    }

    public Long getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    public String getLastHash() {
        return lastHash;
    }

    public void update(Long sequenceNumber, String hash) {
        this.lastSequenceNumber = sequenceNumber;
        this.lastHash = hash;
    }
}