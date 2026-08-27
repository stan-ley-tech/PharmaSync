package com.pharmasync.service.impl;

import com.pharmasync.domain.dispensing.Dispensing;
import com.pharmasync.domain.dispensing.DispensingItem;
import com.pharmasync.domain.dispensing.DispensingStatus;
import com.pharmasync.domain.inventory.InventoryBatch;
import com.pharmasync.domain.prescription.Prescription;
import com.pharmasync.domain.prescription.PrescriptionItem;
import com.pharmasync.domain.prescription.PrescriptionStatus;
import com.pharmasync.domain.user.User;
import com.pharmasync.exception.InvalidStateTransitionException;
import com.pharmasync.exception.ResourceNotFoundException;
import com.pharmasync.kafka.EventPublisher;
import com.pharmasync.kafka.event.MedicineDispensedEvent;
import com.pharmasync.repository.DispensingItemRepository;
import com.pharmasync.repository.DispensingRepository;
import com.pharmasync.repository.InventoryBatchRepository;
import com.pharmasync.repository.PrescriptionItemRepository;
import com.pharmasync.repository.PrescriptionRepository;
import com.pharmasync.repository.UserRepository;
import com.pharmasync.service.DispenseAllocation;
import com.pharmasync.service.DispensingService;
import com.pharmasync.service.InventoryService;
import com.pharmasync.web.dto.DispenseLineRequest;
import com.pharmasync.web.dto.DispenseRequest;
import com.pharmasync.web.dto.DispensingResponse;
import com.pharmasync.web.dto.ReturnRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispensingServiceImpl implements DispensingService {

    private final DispensingRepository dispensingRepository;
    private final DispensingItemRepository dispensingItemRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public DispensingResponse dispense(Long prescriptionId, DispenseRequest request, Long dispensedByUserId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Prescription", prescriptionId));

        if (prescription.getStatus() != PrescriptionStatus.VALIDATED
                && prescription.getStatus() != PrescriptionStatus.PARTIALLY_DISPENSED) {
            throw new InvalidStateTransitionException("Prescription must be validated before dispensing");
        }

        User pharmacist = userRepository.findById(dispensedByUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", dispensedByUserId));

        Dispensing dispensing = new Dispensing();
        dispensing.setDispensingNumber(generateDispensingNumber());
        dispensing.setPrescription(prescription);
        dispensing.setPharmacy(prescription.getPharmacy());
        dispensing.setDispensedBy(pharmacist);
        dispensing.setStatus(DispensingStatus.COMPLETED);
        dispensing.setNotes(request.notes());
        dispensing = dispensingRepository.save(dispensing);

        BigDecimal total = BigDecimal.ZERO;

        for (DispenseLineRequest line : request.items()) {
            PrescriptionItem item = prescriptionItemRepository.findById(line.prescriptionItemId())
                    .orElseThrow(() -> ResourceNotFoundException.of("PrescriptionItem", line.prescriptionItemId()));

            if (!item.getPrescription().getId().equals(prescriptionId)) {
                throw new IllegalArgumentException("Prescription item does not belong to this prescription");
            }
            if (line.quantity() > item.getQuantityOutstanding()) {
                throw new IllegalArgumentException("Requested quantity exceeds the outstanding prescribed quantity");
            }

            List<DispenseAllocation> allocations = inventoryService.consumeReservations(
                    item.getId(), line.quantity(), dispensing.getId(), dispensedByUserId);

            BigDecimal unitPrice = item.getMedicine().getUnitPrice();
            for (DispenseAllocation allocation : allocations) {
                InventoryBatch batch = inventoryBatchRepository.getReferenceById(allocation.inventoryBatchId());

                DispensingItem dispensingItem = new DispensingItem();
                dispensingItem.setPrescriptionItem(item);
                dispensingItem.setInventoryBatch(batch);
                dispensingItem.setQuantity(allocation.quantity());
                dispensingItem.setUnitPrice(unitPrice);
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(allocation.quantity()));
                dispensingItem.setLineTotal(lineTotal);
                dispensing.addItem(dispensingItem);

                total = total.add(lineTotal);
            }

            item.setQuantityDispensed(item.getQuantityDispensed() + line.quantity());
            prescriptionItemRepository.save(item);
        }

        dispensing.setTotalAmount(total);
        dispensing = dispensingRepository.save(dispensing);

        prescription.setStatus(prescription.isFullyDispensed()
                ? PrescriptionStatus.DISPENSED
                : PrescriptionStatus.PARTIALLY_DISPENSED);
        prescriptionRepository.save(prescription);

        eventPublisher.publish(new MedicineDispensedEvent(dispensing.getId(), dispensing.getDispensingNumber(),
                prescriptionId, dispensing.getPharmacy().getId(), dispensedByUserId, total, Instant.now()));

        return DispensingResponse.from(dispensing);
    }

    @Override
    @Transactional
    public DispensingResponse returnItems(Long dispensingId, ReturnRequest request, Long performedByUserId) {
        Dispensing dispensing = dispensingRepository.findById(dispensingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Dispensing", dispensingId));

        DispensingItem item = dispensingItemRepository.findById(request.dispensingItemId())
                .orElseThrow(() -> ResourceNotFoundException.of("DispensingItem", request.dispensingItemId()));

        if (!item.getDispensing().getId().equals(dispensingId)) {
            throw new IllegalArgumentException("Dispensing item does not belong to this dispensing record");
        }

        int returnable = item.getQuantity() - item.getQuantityReturned();
        if (request.quantity() > returnable) {
            throw new IllegalArgumentException("Return quantity exceeds the dispensed quantity still outstanding");
        }

        inventoryService.returnStock(item.getInventoryBatch().getId(), request.quantity(), dispensingId, performedByUserId);
        item.setQuantityReturned(item.getQuantityReturned() + request.quantity());
        dispensingItemRepository.save(item);

        boolean allFullyReturned = dispensing.getItems().stream()
                .allMatch(line -> line.getQuantityReturned() >= line.getQuantity());
        boolean anyReturned = dispensing.getItems().stream().anyMatch(line -> line.getQuantityReturned() > 0);
        dispensing.setStatus(allFullyReturned ? DispensingStatus.RETURNED
                : anyReturned ? DispensingStatus.PARTIALLY_RETURNED
                : DispensingStatus.COMPLETED);

        return DispensingResponse.from(dispensingRepository.save(dispensing));
    }

    @Override
    @Transactional(readOnly = true)
    public DispensingResponse getById(Long id) {
        return dispensingRepository.findById(id)
                .map(DispensingResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Dispensing", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DispensingResponse> findByPharmacy(Long pharmacyId, Pageable pageable) {
        return dispensingRepository.findByPharmacyId(pharmacyId, pageable).map(DispensingResponse::from);
    }

    private String generateDispensingNumber() {
        return "DSP-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
