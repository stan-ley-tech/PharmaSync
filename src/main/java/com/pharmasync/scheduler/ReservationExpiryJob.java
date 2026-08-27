package com.pharmasync.scheduler;

import com.pharmasync.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Releases stock reservations that were never picked up (e.g. a validated prescription
 * whose patient never returned) so the inventory becomes available to others again.
 */
@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final InventoryService inventoryService;

    @Scheduled(cron = "${pharmasync.inventory.reservation-sweep-cron}")
    public void run() {
        int released = inventoryService.sweepExpiredReservations();
        if (released > 0) {
            log.info("Released {} expired inventory reservation(s)", released);
        }
    }
}
