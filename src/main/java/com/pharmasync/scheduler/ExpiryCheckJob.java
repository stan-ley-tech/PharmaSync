package com.pharmasync.scheduler;

import com.pharmasync.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily safety net: marks batches that have already passed their expiry date as EXPIRED
 * and writes off their remaining quantity, then warns about batches expiring soon.
 */
@Component
@RequiredArgsConstructor
public class ExpiryCheckJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiryCheckJob.class);

    private final InventoryService inventoryService;

    @Scheduled(cron = "${pharmasync.inventory.expiry-check-cron}")
    public void run() {
        int expired = inventoryService.sweepExpiredBatches();
        if (expired > 0) {
            log.info("Expiry sweep marked {} batch(es) as expired", expired);
        }
        inventoryService.publishExpiryWarnings();
    }
}
