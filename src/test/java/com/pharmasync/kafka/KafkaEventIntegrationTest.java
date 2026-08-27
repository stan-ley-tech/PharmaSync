package com.pharmasync.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.pharmasync.AbstractIntegrationTest;
import com.pharmasync.kafka.event.MedicineDispensedEvent;
import com.pharmasync.repository.AuditLogRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies the producer/consumer wiring against a real Kafka broker: a published
 * medicine.dispensed event should be picked up by AuditEventListener and land in audit_logs,
 * independent of the request path that originally triggered the dispense.
 */
class KafkaEventIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void medicineDispensedEvent_isConsumedAndAudited() {
        long dispensingId = System.nanoTime();
        MedicineDispensedEvent event = new MedicineDispensedEvent(
                dispensingId, "DSP-KAFKA-TEST", 1L, 1L, 1L, new BigDecimal("12.50"), Instant.now());

        eventPublisher.publish(event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<com.pharmasync.domain.audit.AuditLog> matches =
                    auditLogRepository.findByEntityTypeAndEntityId("Dispensing", dispensingId,
                            org.springframework.data.domain.Pageable.unpaged()).getContent();
            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).getAction()).isEqualTo("MEDICINE_DISPENSED");
        });
    }
}
