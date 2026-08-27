package com.pharmasync.service.impl;

import com.pharmasync.domain.audit.AuditLog;
import com.pharmasync.repository.AuditLogRepository;
import com.pharmasync.service.AuditService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorId, String actorUsername, String action, String entityType, Long entityId,
                        Long pharmacyId, Map<String, Object> details) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setActorUsername(actorUsername);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setPharmacyId(pharmacyId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
