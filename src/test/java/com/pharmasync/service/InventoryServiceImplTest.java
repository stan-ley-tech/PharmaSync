package com.pharmasync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmasync.config.InventoryProperties;
import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.inventory.BatchStatus;
import com.pharmasync.domain.inventory.Inventory;
import com.pharmasync.domain.inventory.InventoryBatch;
import com.pharmasync.domain.inventory.InventoryReservation;
import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.domain.prescription.PrescriptionItem;
import com.pharmasync.exception.InsufficientStockException;
import com.pharmasync.kafka.EventPublisher;
import com.pharmasync.repository.InventoryBatchRepository;
import com.pharmasync.repository.InventoryRepository;
import com.pharmasync.repository.InventoryReservationRepository;
import com.pharmasync.repository.MedicineRepository;
import com.pharmasync.repository.PharmacyRepository;
import com.pharmasync.repository.PrescriptionItemRepository;
import com.pharmasync.repository.PurchaseOrderItemRepository;
import com.pharmasync.repository.StockMovementRepository;
import com.pharmasync.repository.UserRepository;
import com.pharmasync.service.impl.InventoryServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryBatchRepository inventoryBatchRepository;
    @Mock private InventoryReservationRepository reservationRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock private PrescriptionItemRepository prescriptionItemRepository;
    @Mock private EventPublisher eventPublisher;

    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        InventoryProperties properties = new InventoryProperties(
                "0 */15 * * * *", "0 0 6 * * *", "0 */5 * * * *", 30, 60);
        inventoryService = new InventoryServiceImpl(inventoryRepository, inventoryBatchRepository,
                reservationRepository, stockMovementRepository, medicineRepository, pharmacyRepository,
                userRepository, purchaseOrderItemRepository, prescriptionItemRepository, eventPublisher, properties);
    }

    @Test
    void reserve_throwsInsufficientStock_whenAvailableBelowRequested() {
        Inventory inventory = inventoryWith(10, 5); // available = 5
        when(inventoryRepository.lockByPharmacyIdAndMedicineId(1L, 2L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserve(1L, 2L, 99L, 10, 7L))
                .isInstanceOf(InsufficientStockException.class);

        verify(reservationRepository, times(0)).save(any());
    }

    @Test
    void reserve_allocatesAcrossBatchesInExpiryOrder() {
        Inventory inventory = inventoryWith(100, 0);
        InventoryBatch soonExpiring = batch(1L, inventory, 30, LocalDate.now().plusDays(5));
        InventoryBatch laterExpiring = batch(2L, inventory, 100, LocalDate.now().plusDays(60));

        when(inventoryRepository.lockByPharmacyIdAndMedicineId(1L, 2L)).thenReturn(Optional.of(inventory));
        when(inventoryBatchRepository.lockAvailableBatchesForDispensing(inventory.getId()))
                .thenReturn(List.of(soonExpiring, laterExpiring));
        when(reservationRepository.sumActiveQuantityByBatchId(anyLong())).thenReturn(0);
        when(prescriptionItemRepository.getReferenceById(99L)).thenReturn(new PrescriptionItem());

        inventoryService.reserve(1L, 2L, 99L, 50, 7L);

        ArgumentCaptor<InventoryReservation> captor = ArgumentCaptor.forClass(InventoryReservation.class);
        verify(reservationRepository, times(2)).save(captor.capture());

        List<InventoryReservation> saved = captor.getAllValues();
        assertThat(saved.get(0).getInventoryBatch()).isEqualTo(soonExpiring);
        assertThat(saved.get(0).getQuantity()).isEqualTo(30);
        assertThat(saved.get(1).getInventoryBatch()).isEqualTo(laterExpiring);
        assertThat(saved.get(1).getQuantity()).isEqualTo(20);

        assertThat(inventory.getQuantityReserved()).isEqualTo(50);
    }

    @Test
    void adjustStock_rejectsAdjustmentThatWouldGoBelowZero() {
        Inventory inventory = inventoryWith(10, 0);
        InventoryBatch batch = batch(1L, inventory, 5, LocalDate.now().plusDays(10));

        when(inventoryBatchRepository.lockById(1L)).thenReturn(Optional.of(batch));
        when(inventoryRepository.lockById(inventory.getId())).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, -20, "damaged", 7L))
                .isInstanceOf(InsufficientStockException.class);
    }

    private Inventory inventoryWith(int onHand, int reserved) {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setPharmacy(new Pharmacy());
        Medicine medicine = new Medicine();
        medicine.setId(2L);
        medicine.setName("Paracetamol 500mg");
        medicine.setReorderThreshold(10);
        inventory.setMedicine(medicine);
        inventory.setQuantityOnHand(onHand);
        inventory.setQuantityReserved(reserved);
        return inventory;
    }

    private InventoryBatch batch(Long id, Inventory inventory, int remaining, LocalDate expiryDate) {
        InventoryBatch batch = new InventoryBatch();
        batch.setId(id);
        batch.setInventory(inventory);
        batch.setBatchNumber("BATCH-" + id);
        batch.setQuantityReceived(remaining);
        batch.setQuantityRemaining(remaining);
        batch.setUnitCost(BigDecimal.ONE);
        batch.setExpiryDate(expiryDate);
        batch.setStatus(BatchStatus.ACTIVE);
        return batch;
    }
}
