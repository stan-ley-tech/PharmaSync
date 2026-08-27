package com.pharmasync.kafka.consumer;

import com.pharmasync.kafka.KafkaTopics;
import com.pharmasync.kafka.event.InventoryLowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Stands in for a dedicated notification service: reacts to low-stock signals by
 * "sending" an alert to the branch's inventory manager. In this demo the notification
 * channel is a structured log line rather than a real email/SMS provider.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @KafkaListener(topics = KafkaTopics.INVENTORY_LOW, groupId = "pharmasync-notification-service")
    public void onInventoryLow(InventoryLowEvent event) {
        log.warn("[notification] Low stock at pharmacy {}: {} has {} units available (reorder threshold {})",
                event.pharmacyId(), event.medicineName(), event.quantityAvailable(), event.reorderThreshold());
    }
}
