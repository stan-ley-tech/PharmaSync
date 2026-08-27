package com.pharmasync.repository;

import com.pharmasync.domain.inventory.BatchStatus;
import com.pharmasync.domain.inventory.InventoryBatch;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    List<InventoryBatch> findByInventoryIdOrderByExpiryDateAsc(Long inventoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM InventoryBatch b
            WHERE b.inventory.id = :inventoryId
              AND b.status = 'ACTIVE'
              AND b.quantityRemaining > 0
            ORDER BY b.expiryDate ASC
            """)
    List<InventoryBatch> lockAvailableBatchesForDispensing(@Param("inventoryId") Long inventoryId);

    @Query("""
            SELECT b FROM InventoryBatch b
            WHERE b.status = 'ACTIVE'
              AND b.expiryDate <= :cutoffDate
            """)
    List<InventoryBatch> findActiveBatchesExpiringBy(@Param("cutoffDate") LocalDate cutoffDate);

    List<InventoryBatch> findByStatus(BatchStatus status);
}
