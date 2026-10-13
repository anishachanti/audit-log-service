package com.anish.auditlogservice;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditChainHeadInitializer implements ApplicationRunner {

    private final AuditChainHeadRepository chainHeadRepository;
    private final AuditEventRepository auditEventRepository;

    public AuditChainHeadInitializer(
            AuditChainHeadRepository chainHeadRepository,
            AuditEventRepository auditEventRepository) {
        this.chainHeadRepository = chainHeadRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (chainHeadRepository.existsById(AuditChainHeadEntity.SINGLETON_ID)) {
            return;
        }

        AuditEventEntity latestEvent = auditEventRepository
                .findTopByOrderBySequenceNumberDesc()
                .orElse(null);

        AuditChainHeadEntity head;

        if (latestEvent == null) {
            head = AuditChainHeadEntity.initial();
        } else {
            head = new AuditChainHeadEntity(
                    AuditChainHeadEntity.SINGLETON_ID,
                    latestEvent.getSequenceNumber(),
                    latestEvent.getHash());
        }

        chainHeadRepository.saveAndFlush(head);
    }
}