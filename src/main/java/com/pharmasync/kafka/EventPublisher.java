package com.pharmasync.kafka;

import com.pharmasync.kafka.event.InventoryLowEvent;
import com.pharmasync.kafka.event.InventoryReservedEvent;
import com.pharmasync.kafka.event.InventoryTransferredEvent;
import com.pharmasync.kafka.event.MedicineDispensedEvent;
import com.pharmasync.kafka.event.MedicineExpiringEvent;
import com.pharmasync.kafka.event.PrescriptionCreatedEvent;
import com.pharmasync.kafka.event.PrescriptionValidatedEvent;
import com.pharmasync.kafka.event.PurchaseReceivedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(PrescriptionCreatedEvent event) {
        send(KafkaTopics.PRESCRIPTION_CREATED, event.prescriptionId(), event);
    }

    public void publish(PrescriptionValidatedEvent event) {
        send(KafkaTopics.PRESCRIPTION_VALIDATED, event.prescriptionId(), event);
    }

    public void publish(InventoryReservedEvent event) {
        send(KafkaTopics.INVENTORY_RESERVED, event.medicineId(), event);
    }

    public void publish(MedicineDispensedEvent event) {
        send(KafkaTopics.MEDICINE_DISPENSED, event.dispensingId(), event);
    }

    public void publish(InventoryLowEvent event) {
        send(KafkaTopics.INVENTORY_LOW, event.medicineId(), event);
    }

    public void publish(MedicineExpiringEvent event) {
        send(KafkaTopics.MEDICINE_EXPIRING, event.medicineId(), event);
    }

    public void publish(PurchaseReceivedEvent event) {
        send(KafkaTopics.PURCHASE_RECEIVED, event.purchaseOrderId(), event);
    }

    public void publish(InventoryTransferredEvent event) {
        send(KafkaTopics.INVENTORY_TRANSFERRED, event.medicineId(), event);
    }

    private void send(String topic, Object key, Object payload) {
        String partitionKey = String.valueOf(key);
        kafkaTemplate.send(topic, partitionKey, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish event to topic {} with key {}", topic, partitionKey, ex);
                    }
                });
    }
}
