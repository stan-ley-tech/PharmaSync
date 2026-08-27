package com.pharmasync.service;

import com.pharmasync.domain.inventory.Inventory;
import com.pharmasync.domain.inventory.InventoryBatch;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    Inventory getByPharmacyAndMedicine(Long pharmacyId, Long medicineId);

    Page<Inventory> findByPharmacy(Long pharmacyId, Pageable pageable);

    InventoryBatch receiveStock(ReceiveStockCommand command);

    void reserve(Long pharmacyId, Long medicineId, Long prescriptionItemId, int quantity, Long performedByUserId);

    void releaseReservationsForPrescriptionItem(Long prescriptionItemId, Long performedByUserId);

    List<DispenseAllocation> consumeReservations(Long prescriptionItemId, int quantity, Long dispensingId,
                                                   Long performedByUserId);

    void returnStock(Long inventoryBatchId, int quantity, Long dispensingId, Long performedByUserId);

    void adjustStock(Long inventoryBatchId, int delta, String reason, Long performedByUserId);

    void transferStock(Long medicineId, Long fromPharmacyId, Long toPharmacyId, int quantity, Long performedByUserId);

    List<Inventory> findLowStockInventory();

    int sweepExpiredBatches();

    int sweepExpiredReservations();

    void publishExpiryWarnings();
}
