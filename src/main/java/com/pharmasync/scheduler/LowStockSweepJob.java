package com.pharmasync.scheduler;

import com.pharmasync.kafka.EventPublisher;
import com.pharmasync.kafka.event.InventoryLowEvent;
import com.pharmasync.service.InventoryService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic safety net that catches anything the real-time low-stock check on the
 * dispense/adjust path might have missed, e.g. after a reorder threshold is lowered.
 */
@Component
@RequiredArgsConstructor
public class LowStockSweepJob {

    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;

    @Scheduled(cron = "${pharmasync.inventory.low-stock-check-cron}")
    public void run() {
        inventoryService.findLowStockInventory().forEach(inventory -> eventPublisher.publish(new InventoryLowEvent(
                inventory.pharmacyId(),
                inventory.medicineId(),
                inventory.medicineName(),
                inventory.quantityAvailable(),
                inventory.reorderThreshold(),
                Instant.now())));
    }
}
