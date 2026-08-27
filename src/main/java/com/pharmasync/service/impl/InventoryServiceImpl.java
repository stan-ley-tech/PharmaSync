package com.pharmasync.service.impl;

import com.pharmasync.config.InventoryProperties;
import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.inventory.BatchStatus;
import com.pharmasync.domain.inventory.Inventory;
import com.pharmasync.domain.inventory.InventoryBatch;
import com.pharmasync.domain.inventory.InventoryReservation;
import com.pharmasync.domain.inventory.MovementType;
import com.pharmasync.domain.inventory.ReservationStatus;
import com.pharmasync.domain.inventory.StockMovement;
import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.exception.InsufficientStockException;
import com.pharmasync.exception.ResourceNotFoundException;
import com.pharmasync.kafka.EventPublisher;
import com.pharmasync.kafka.event.InventoryLowEvent;
import com.pharmasync.kafka.event.InventoryTransferredEvent;
import com.pharmasync.kafka.event.MedicineExpiringEvent;
import com.pharmasync.repository.InventoryBatchRepository;
import com.pharmasync.repository.InventoryRepository;
import com.pharmasync.repository.InventoryReservationRepository;
import com.pharmasync.repository.MedicineRepository;
import com.pharmasync.repository.PharmacyRepository;
import com.pharmasync.repository.PrescriptionItemRepository;
import com.pharmasync.repository.PurchaseOrderItemRepository;
import com.pharmasync.repository.StockMovementRepository;
import com.pharmasync.repository.UserRepository;
import com.pharmasync.service.DispenseAllocation;
import com.pharmasync.service.InventoryService;
import com.pharmasync.service.ReceiveStockCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryReservationRepository reservationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final MedicineRepository medicineRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final EventPublisher eventPublisher;
    private final InventoryProperties inventoryProperties;

    @Override
    @Transactional(readOnly = true)
    public Inventory getByPharmacyAndMedicine(Long pharmacyId, Long medicineId) {
        return inventoryRepository.findByPharmacyIdAndMedicineId(pharmacyId, medicineId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record for medicine " + medicineId + " at pharmacy " + pharmacyId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Inventory> findByPharmacy(Long pharmacyId, Pageable pageable) {
        return inventoryRepository.findByPharmacyId(pharmacyId, pageable);
    }

    @Override
    @Transactional
    public InventoryBatch receiveStock(ReceiveStockCommand command) {
        Inventory inventory = findOrCreateInventory(command.pharmacyId(), command.medicineId());

        InventoryBatch batch = new InventoryBatch();
        batch.setInventory(inventory);
        batch.setBatchNumber(command.batchNumber());
        batch.setQuantityReceived(command.quantity());
        batch.setQuantityRemaining(command.quantity());
        batch.setUnitCost(command.unitCost());
        batch.setManufacturedDate(command.manufacturedDate());
        batch.setExpiryDate(command.expiryDate());
        batch.setStatus(BatchStatus.ACTIVE);
        if (command.purchaseOrderItemId() != null) {
            batch.setPurchaseOrderItem(purchaseOrderItemRepository.getReferenceById(command.purchaseOrderItemId()));
        }
        batch = inventoryBatchRepository.save(batch);

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + command.quantity());
        inventoryRepository.save(inventory);

        MovementType movementType = command.purchaseOrderItemId() != null ? MovementType.PURCHASE : MovementType.RECEIPT;
        recordMovement(batch, movementType, command.quantity(), 0, command.quantity(),
                movementType == MovementType.PURCHASE ? "PURCHASE_ORDER_ITEM" : "MANUAL_RECEIPT",
                command.purchaseOrderItemId(), command.performedByUserId(), null);

        return batch;
    }

    @Override
    @Transactional
    public void reserve(Long pharmacyId, Long medicineId, Long prescriptionItemId, int quantity, Long performedByUserId) {
        Inventory inventory = inventoryRepository.lockByPharmacyIdAndMedicineId(pharmacyId, medicineId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record for medicine " + medicineId + " at pharmacy " + pharmacyId));

        if (inventory.getQuantityAvailable() < quantity) {
            throw new InsufficientStockException("Insufficient available stock for medicine " + medicineId
                    + ": requested " + quantity + ", available " + inventory.getQuantityAvailable());
        }

        List<InventoryBatch> batches = inventoryBatchRepository.lockAvailableBatchesForDispensing(inventory.getId());
        int remaining = quantity;

        for (InventoryBatch batch : batches) {
            if (remaining <= 0) {
                break;
            }
            int alreadyReserved = reservationRepository.sumActiveQuantityByBatchId(batch.getId());
            int freeInBatch = batch.getQuantityRemaining() - alreadyReserved;
            if (freeInBatch <= 0) {
                continue;
            }
            int allocation = Math.min(freeInBatch, remaining);

            InventoryReservation reservation = new InventoryReservation();
            reservation.setPrescriptionItem(prescriptionItemRepository.getReferenceById(prescriptionItemId));
            reservation.setInventoryBatch(batch);
            reservation.setQuantity(allocation);
            reservation.setStatus(ReservationStatus.ACTIVE);
            reservation.setReservedAt(Instant.now());
            reservation.setExpiresAt(Instant.now().plusSeconds(inventoryProperties.reservationTtlMinutes() * 60));
            reservationRepository.save(reservation);

            recordMovement(batch, MovementType.RESERVATION, -allocation, batch.getQuantityRemaining(),
                    batch.getQuantityRemaining(), "PRESCRIPTION_ITEM", prescriptionItemId, performedByUserId, null);

            remaining -= allocation;
        }

        if (remaining > 0) {
            throw new InsufficientStockException("Unable to allocate the full requested quantity across available batches");
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void releaseReservationsForPrescriptionItem(Long prescriptionItemId, Long performedByUserId) {
        List<InventoryReservation> active =
                reservationRepository.findByPrescriptionItemIdAndStatus(prescriptionItemId, ReservationStatus.ACTIVE);
        releaseReservations(active, "PRESCRIPTION_CANCELLED", performedByUserId);
    }

    @Override
    @Transactional
    public List<DispenseAllocation> consumeReservations(Long prescriptionItemId, int quantity, Long dispensingId,
                                                          Long performedByUserId) {
        List<InventoryReservation> active =
                reservationRepository.findByPrescriptionItemIdAndStatus(prescriptionItemId, ReservationStatus.ACTIVE);
        active.sort(Comparator.comparing(r -> r.getInventoryBatch().getExpiryDate()));

        if (active.isEmpty()) {
            throw new InsufficientStockException("No active reservation found for prescription item " + prescriptionItemId);
        }

        Inventory inventory = inventoryRepository.lockById(active.get(0).getInventoryBatch().getInventory().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        List<DispenseAllocation> allocations = new ArrayList<>();
        int remaining = quantity;

        for (InventoryReservation staleReservation : active) {
            if (remaining <= 0) {
                break;
            }
            InventoryBatch batch = inventoryBatchRepository.lockById(staleReservation.getInventoryBatch().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory batch not found"));

            // Re-read after acquiring the batch lock: a concurrent release/consume on this same
            // reservation could have committed between the initial query above and this point,
            // and the batch lock is what makes this read authoritative rather than stale.
            InventoryReservation reservation = reservationRepository.findById(staleReservation.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory reservation not found"));
            if (reservation.getStatus() != ReservationStatus.ACTIVE || reservation.getQuantity() <= 0) {
                continue;
            }

            int consumeQty = Math.min(reservation.getQuantity(), remaining);
            int before = batch.getQuantityRemaining();
            batch.setQuantityRemaining(before - consumeQty);
            if (batch.getQuantityRemaining() == 0) {
                batch.setStatus(BatchStatus.DEPLETED);
            }
            inventoryBatchRepository.save(batch);

            reservation.setQuantity(reservation.getQuantity() - consumeQty);
            reservation.setStatus(reservation.getQuantity() == 0 ? ReservationStatus.CONSUMED : ReservationStatus.ACTIVE);
            reservationRepository.save(reservation);

            recordMovement(batch, MovementType.DISPENSE, -consumeQty, before, batch.getQuantityRemaining(),
                    "DISPENSING", dispensingId, performedByUserId, null);

            allocations.add(new DispenseAllocation(batch.getId(), batch.getBatchNumber(), consumeQty));
            remaining -= consumeQty;
        }

        if (remaining > 0) {
            throw new InsufficientStockException("Reserved quantity is less than the requested dispense quantity");
        }

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - quantity);
        inventory.setQuantityReserved(inventory.getQuantityReserved() - quantity);
        inventoryRepository.save(inventory);

        checkLowStock(inventory);

        return allocations;
    }

    @Override
    @Transactional
    public void returnStock(Long inventoryBatchId, int quantity, Long dispensingId, Long performedByUserId) {
        InventoryBatch batch = inventoryBatchRepository.lockById(inventoryBatchId)
                .orElseThrow(() -> ResourceNotFoundException.of("InventoryBatch", inventoryBatchId));
        Inventory inventory = inventoryRepository.lockById(batch.getInventory().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        int before = batch.getQuantityRemaining();
        batch.setQuantityRemaining(before + quantity);
        if (batch.getStatus() == BatchStatus.DEPLETED) {
            batch.setStatus(batch.isExpired(LocalDate.now()) ? BatchStatus.EXPIRED : BatchStatus.ACTIVE);
        }
        inventoryBatchRepository.save(batch);

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + quantity);
        inventoryRepository.save(inventory);

        recordMovement(batch, MovementType.RETURN, quantity, before, batch.getQuantityRemaining(),
                "DISPENSING", dispensingId, performedByUserId, null);
    }

    @Override
    @Transactional
    public void adjustStock(Long inventoryBatchId, int delta, String reason, Long performedByUserId) {
        InventoryBatch batch = inventoryBatchRepository.lockById(inventoryBatchId)
                .orElseThrow(() -> ResourceNotFoundException.of("InventoryBatch", inventoryBatchId));
        Inventory inventory = inventoryRepository.lockById(batch.getInventory().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        int newRemaining = batch.getQuantityRemaining() + delta;
        if (newRemaining < 0) {
            throw new InsufficientStockException("Adjustment would drive batch " + inventoryBatchId + " below zero");
        }
        int newOnHand = inventory.getQuantityOnHand() + delta;
        if (newOnHand < inventory.getQuantityReserved()) {
            throw new InsufficientStockException("Adjustment would drive on-hand stock below already reserved stock");
        }

        int before = batch.getQuantityRemaining();
        batch.setQuantityRemaining(newRemaining);
        batch.setStatus(newRemaining == 0 ? BatchStatus.DEPLETED : BatchStatus.ACTIVE);
        inventoryBatchRepository.save(batch);

        inventory.setQuantityOnHand(newOnHand);
        inventoryRepository.save(inventory);

        recordMovement(batch, MovementType.ADJUSTMENT, delta, before, newRemaining, "ADJUSTMENT", null,
                performedByUserId, reason);

        if (delta < 0) {
            checkLowStock(inventory);
        }
    }

    @Override
    @Transactional
    public void transferStock(Long medicineId, Long fromPharmacyId, Long toPharmacyId, int quantity, Long performedByUserId) {
        if (fromPharmacyId.equals(toPharmacyId)) {
            throw new IllegalArgumentException("Source and destination pharmacy must differ");
        }

        Long firstLockPharmacyId = Math.min(fromPharmacyId, toPharmacyId);
        Long secondLockPharmacyId = Math.max(fromPharmacyId, toPharmacyId);
        Inventory first = findOrCreateInventory(firstLockPharmacyId, medicineId);
        Inventory second = findOrCreateInventory(secondLockPharmacyId, medicineId);

        Inventory source = fromPharmacyId.equals(firstLockPharmacyId) ? first : second;
        Inventory destination = fromPharmacyId.equals(firstLockPharmacyId) ? second : first;

        if (source.getQuantityAvailable() < quantity) {
            throw new InsufficientStockException("Insufficient available stock to transfer for medicine " + medicineId);
        }

        List<InventoryBatch> batches = inventoryBatchRepository.lockAvailableBatchesForDispensing(source.getId());
        int remaining = quantity;

        for (InventoryBatch sourceBatch : batches) {
            if (remaining <= 0) {
                break;
            }
            int alreadyReserved = reservationRepository.sumActiveQuantityByBatchId(sourceBatch.getId());
            int freeInBatch = sourceBatch.getQuantityRemaining() - alreadyReserved;
            if (freeInBatch <= 0) {
                continue;
            }
            int allocation = Math.min(freeInBatch, remaining);

            int before = sourceBatch.getQuantityRemaining();
            sourceBatch.setQuantityRemaining(before - allocation);
            if (sourceBatch.getQuantityRemaining() == 0) {
                sourceBatch.setStatus(BatchStatus.DEPLETED);
            }
            inventoryBatchRepository.save(sourceBatch);
            recordMovement(sourceBatch, MovementType.TRANSFER, -allocation, before, sourceBatch.getQuantityRemaining(),
                    "TRANSFER", toPharmacyId, performedByUserId, "Transfer to pharmacy " + toPharmacyId);

            InventoryBatch destinationBatch = new InventoryBatch();
            destinationBatch.setInventory(destination);
            destinationBatch.setBatchNumber(sourceBatch.getBatchNumber());
            destinationBatch.setQuantityReceived(allocation);
            destinationBatch.setQuantityRemaining(allocation);
            destinationBatch.setUnitCost(sourceBatch.getUnitCost());
            destinationBatch.setManufacturedDate(sourceBatch.getManufacturedDate());
            destinationBatch.setExpiryDate(sourceBatch.getExpiryDate());
            destinationBatch.setStatus(BatchStatus.ACTIVE);
            destinationBatch = inventoryBatchRepository.save(destinationBatch);
            recordMovement(destinationBatch, MovementType.TRANSFER, allocation, 0, allocation,
                    "TRANSFER", fromPharmacyId, performedByUserId, "Transfer from pharmacy " + fromPharmacyId);

            remaining -= allocation;
        }

        if (remaining > 0) {
            throw new InsufficientStockException("Unable to allocate the full transfer quantity across available batches");
        }

        source.setQuantityOnHand(source.getQuantityOnHand() - quantity);
        destination.setQuantityOnHand(destination.getQuantityOnHand() + quantity);
        inventoryRepository.save(source);
        inventoryRepository.save(destination);

        checkLowStock(source);

        eventPublisher.publish(new InventoryTransferredEvent(medicineId, fromPharmacyId, toPharmacyId, quantity, Instant.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findLowStockInventory() {
        return inventoryRepository.findAllBelowReorderThreshold();
    }

    @Override
    @Transactional
    public int sweepExpiredBatches() {
        List<InventoryBatch> expired = inventoryBatchRepository.findActiveBatchesExpiringBy(LocalDate.now());
        int count = 0;

        for (InventoryBatch batchRef : expired) {
            InventoryBatch batch = inventoryBatchRepository.lockById(batchRef.getId()).orElse(null);
            if (batch == null || batch.getStatus() != BatchStatus.ACTIVE) {
                continue;
            }
            Inventory inventory = inventoryRepository.lockById(batch.getInventory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

            List<InventoryReservation> activeReservations =
                    reservationRepository.findByInventoryBatchIdAndStatus(batch.getId(), ReservationStatus.ACTIVE);
            int reservedOnBatch = activeReservations.stream().mapToInt(InventoryReservation::getQuantity).sum();
            for (InventoryReservation reservation : activeReservations) {
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservation.setReleasedAt(Instant.now());
                reservationRepository.save(reservation);
            }

            int before = batch.getQuantityRemaining();
            batch.setStatus(BatchStatus.EXPIRED);
            batch.setQuantityRemaining(0);
            inventoryBatchRepository.save(batch);

            inventory.setQuantityOnHand(inventory.getQuantityOnHand() - before);
            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservedOnBatch);
            inventoryRepository.save(inventory);

            recordMovement(batch, MovementType.EXPIRY, -before, before, 0, "EXPIRY", null, null, null);
            checkLowStock(inventory);
            count++;
        }

        return count;
    }

    @Override
    @Transactional
    public int sweepExpiredReservations() {
        List<InventoryReservation> expired =
                reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, Instant.now());
        releaseReservations(expired, "RESERVATION_TTL_EXPIRED", null);
        return expired.size();
    }

    @Override
    @Transactional(readOnly = true)
    public void publishExpiryWarnings() {
        LocalDate cutoff = LocalDate.now().plusDays(inventoryProperties.expiryWarningDays());
        List<InventoryBatch> soonExpiring = inventoryBatchRepository.findActiveBatchesExpiringBy(cutoff);

        for (InventoryBatch batch : soonExpiring) {
            if (!batch.getExpiryDate().isAfter(LocalDate.now())) {
                continue;
            }
            Medicine medicine = batch.getInventory().getMedicine();
            eventPublisher.publish(new MedicineExpiringEvent(batch.getId(), medicine.getId(), medicine.getName(),
                    batch.getBatchNumber(), batch.getQuantityRemaining(), batch.getExpiryDate(), Instant.now()));
        }
    }

    private void releaseReservations(List<InventoryReservation> reservations, String reason, Long performedByUserId) {
        Map<Long, Integer> releasedByInventoryId = new HashMap<>();

        for (InventoryReservation staleReservation : reservations) {
            InventoryBatch batch = inventoryBatchRepository.lockById(staleReservation.getInventoryBatch().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory batch not found"));

            // See the equivalent re-read in consumeReservations: the batch lock only makes this
            // authoritative once we re-fetch, since the row itself carries no lock of its own.
            InventoryReservation reservation = reservationRepository.findById(staleReservation.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory reservation not found"));
            if (reservation.getStatus() != ReservationStatus.ACTIVE || reservation.getQuantity() <= 0) {
                continue;
            }

            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setReleasedAt(Instant.now());
            reservationRepository.save(reservation);

            recordMovement(batch, MovementType.RELEASE, reservation.getQuantity(), batch.getQuantityRemaining(),
                    batch.getQuantityRemaining(), "RESERVATION", reservation.getId(), performedByUserId, reason);

            releasedByInventoryId.merge(batch.getInventory().getId(), reservation.getQuantity(), Integer::sum);
        }

        releasedByInventoryId.forEach((inventoryId, releasedQuantity) -> {
            Inventory inventory = inventoryRepository.lockById(inventoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));
            inventory.setQuantityReserved(inventory.getQuantityReserved() - releasedQuantity);
            inventoryRepository.save(inventory);
        });
    }

    private void checkLowStock(Inventory inventory) {
        if (inventory.getQuantityAvailable() <= inventory.effectiveReorderThreshold()) {
            eventPublisher.publish(new InventoryLowEvent(
                    inventory.getPharmacy().getId(),
                    inventory.getMedicine().getId(),
                    inventory.getMedicine().getName(),
                    inventory.getQuantityAvailable(),
                    inventory.effectiveReorderThreshold(),
                    Instant.now()));
        }
    }

    private Inventory findOrCreateInventory(Long pharmacyId, Long medicineId) {
        return inventoryRepository.lockByPharmacyIdAndMedicineId(pharmacyId, medicineId)
                .orElseGet(() -> createInventory(pharmacyId, medicineId));
    }

    private Inventory createInventory(Long pharmacyId, Long medicineId) {
        try {
            Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Pharmacy", pharmacyId));
            Medicine medicine = medicineRepository.findById(medicineId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Medicine", medicineId));

            Inventory inventory = new Inventory();
            inventory.setPharmacy(pharmacy);
            inventory.setMedicine(medicine);
            inventory.setQuantityOnHand(0);
            inventory.setQuantityReserved(0);
            return inventoryRepository.saveAndFlush(inventory);
        } catch (DataIntegrityViolationException raceOnCreate) {
            log.debug("Concurrent inventory row creation detected for pharmacy {} medicine {}, re-reading",
                    pharmacyId, medicineId);
            return inventoryRepository.lockByPharmacyIdAndMedicineId(pharmacyId, medicineId)
                    .orElseThrow(() -> raceOnCreate);
        }
    }

    private void recordMovement(InventoryBatch batch, MovementType type, int quantity, int before, int after,
                                 String referenceType, Long referenceId, Long performedByUserId, String notes) {
        StockMovement movement = new StockMovement();
        movement.setInventoryBatch(batch);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setQuantityBefore(before);
        movement.setQuantityAfter(after);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setNotes(notes);
        if (performedByUserId != null) {
            movement.setPerformedBy(userRepository.getReferenceById(performedByUserId));
        }
        stockMovementRepository.save(movement);
    }
}
