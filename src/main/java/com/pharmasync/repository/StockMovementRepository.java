package com.pharmasync.repository;

import com.pharmasync.domain.inventory.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByInventoryBatchIdOrderByCreatedAtDesc(Long inventoryBatchId, Pageable pageable);

    Page<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId, Pageable pageable);
}
