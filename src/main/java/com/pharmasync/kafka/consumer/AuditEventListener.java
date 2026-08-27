package com.pharmasync.kafka.consumer;

import com.pharmasync.kafka.KafkaTopics;
import com.pharmasync.kafka.event.MedicineDispensedEvent;
import com.pharmasync.service.AuditService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Independent audit trail consumer: even if the dispensing request path is later changed,
 * every completed dispense is still recorded here from the event stream.
 */
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditService auditService;

    @KafkaListener(topics = KafkaTopics.MEDICINE_DISPENSED, groupId = "pharmasync-audit-service")
    public void onMedicineDispensed(MedicineDispensedEvent event) {
        auditService.record(
                event.dispensedByUserId(),
                null,
                "MEDICINE_DISPENSED",
                "Dispensing",
                event.dispensingId(),
                event.pharmacyId(),
                Map.of(
                        "dispensingNumber", event.dispensingNumber(),
                        "prescriptionId", event.prescriptionId(),
                        "totalAmount", event.totalAmount().toString()));
    }
}
