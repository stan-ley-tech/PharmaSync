package com.pharmasync.scheduler;

import com.pharmasync.domain.inventory.Inventory;
import com.pharmasync.kafka.EventPublisher;
import com.pharmasync.kafka.event.InventoryLowEvent;
import com.pharmasync.service.InventoryService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public void run() {
        for (Inventory inventory : inventoryService.findLowStockInventory()) {
            eventPublisher.publish(new InventoryLowEvent(
                    inventory.getPharmacy().getId(),
                    inventory.getMedicine().getId(),
                    inventory.getMedicine().getName(),
                    inventory.getQuantityAvailable(),
                    inventory.effectiveReorderThreshold(),
                    Instant.now()));
        }
    }
}
