package com.pharmasync.kafka.consumer;

import com.pharmasync.kafka.KafkaTopics;
import com.pharmasync.kafka.event.MedicineExpiringEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExpiryAlertListener {

    private static final Logger log = LoggerFactory.getLogger(ExpiryAlertListener.class);

    @KafkaListener(topics = KafkaTopics.MEDICINE_EXPIRING, groupId = "pharmasync-expiry-alert-worker")
    public void onMedicineExpiring(MedicineExpiringEvent event) {
        log.warn("[expiry-alert] Batch {} of {} ({} units) expires on {}",
                event.batchNumber(), event.medicineName(), event.quantityRemaining(), event.expiryDate());
    }
}
