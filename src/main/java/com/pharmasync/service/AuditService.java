package com.pharmasync.service;

import java.util.Map;

public interface AuditService {

    void record(Long actorId, String actorUsername, String action, String entityType, Long entityId,
                Long pharmacyId, Map<String, Object> details);
}
