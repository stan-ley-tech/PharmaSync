package com.pharmasync.repository;

import com.pharmasync.domain.procurement.PurchaseOrder;
import com.pharmasync.domain.procurement.PurchaseOrderStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    Page<PurchaseOrder> findByPharmacyIdAndStatus(Long pharmacyId, PurchaseOrderStatus status, Pageable pageable);

    Page<PurchaseOrder> findByPharmacyId(Long pharmacyId, Pageable pageable);
}
