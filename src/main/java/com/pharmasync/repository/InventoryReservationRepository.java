package com.pharmasync.repository;

import com.pharmasync.domain.inventory.InventoryReservation;
import com.pharmasync.domain.inventory.ReservationStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findByPrescriptionItemIdAndStatus(Long prescriptionItemId, ReservationStatus status);

    List<InventoryReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);

    List<InventoryReservation> findByInventoryBatchIdAndStatus(Long inventoryBatchId, ReservationStatus status);

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0) FROM InventoryReservation r
            WHERE r.inventoryBatch.id = :batchId AND r.status = 'ACTIVE'
            """)
    int sumActiveQuantityByBatchId(@Param("batchId") Long batchId);
}
