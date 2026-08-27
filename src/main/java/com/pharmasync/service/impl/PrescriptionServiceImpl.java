package com.pharmasync.service.impl;

import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.domain.prescription.Prescription;
import com.pharmasync.domain.prescription.PrescriptionItem;
import com.pharmasync.domain.prescription.PrescriptionStatus;
import com.pharmasync.domain.user.User;
import com.pharmasync.exception.InvalidStateTransitionException;
import com.pharmasync.exception.ResourceNotFoundException;
import com.pharmasync.kafka.EventPublisher;
import com.pharmasync.kafka.event.PrescriptionCreatedEvent;
import com.pharmasync.kafka.event.PrescriptionValidatedEvent;
import com.pharmasync.repository.MedicineRepository;
import com.pharmasync.repository.PharmacyRepository;
import com.pharmasync.repository.PrescriptionRepository;
import com.pharmasync.repository.UserRepository;
import com.pharmasync.service.InventoryService;
import com.pharmasync.service.PrescriptionService;
import com.pharmasync.web.dto.CreatePrescriptionRequest;
import com.pharmasync.web.dto.PrescriptionLineRequest;
import com.pharmasync.web.dto.PrescriptionResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public PrescriptionResponse create(CreatePrescriptionRequest request) {
        Pharmacy pharmacy = pharmacyRepository.findById(request.pharmacyId())
                .orElseThrow(() -> ResourceNotFoundException.of("Pharmacy", request.pharmacyId()));

        Prescription prescription = new Prescription();
        prescription.setPrescriptionNumber(generatePrescriptionNumber());
        prescription.setPharmacy(pharmacy);
        prescription.setPatientName(request.patientName());
        prescription.setPatientIdentifier(request.patientIdentifier());
        prescription.setPatientContact(request.patientContact());
        prescription.setPrescriberName(request.prescriberName());
        prescription.setPrescriberLicense(request.prescriberLicense());
        prescription.setIssuedDate(request.issuedDate());
        prescription.setStatus(PrescriptionStatus.CREATED);

        if (request.prescribedByUserId() != null) {
            User prescriber = userRepository.findById(request.prescribedByUserId())
                    .orElseThrow(() -> ResourceNotFoundException.of("User", request.prescribedByUserId()));
            prescription.setPrescribedBy(prescriber);
        }

        for (PrescriptionLineRequest line : request.items()) {
            Medicine medicine = medicineRepository.findById(line.medicineId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Medicine", line.medicineId()));

            PrescriptionItem item = new PrescriptionItem();
            item.setMedicine(medicine);
            item.setQuantityPrescribed(line.quantity());
            item.setDosageInstructions(line.dosageInstructions());
            item.setSubstitutionAllowed(line.substitutionAllowed());
            prescription.addItem(item);
        }

        prescription = prescriptionRepository.save(prescription);

        eventPublisher.publish(new PrescriptionCreatedEvent(prescription.getId(), prescription.getPrescriptionNumber(),
                pharmacy.getId(), prescription.getPatientName(), Instant.now()));

        return PrescriptionResponse.from(prescription);
    }

    @Override
    @Transactional
    public PrescriptionResponse validate(Long prescriptionId, Long validatedByUserId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Prescription", prescriptionId));

        if (prescription.getStatus() != PrescriptionStatus.CREATED) {
            throw new InvalidStateTransitionException("Only newly created prescriptions can be validated");
        }

        User validator = userRepository.findById(validatedByUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", validatedByUserId));

        for (PrescriptionItem item : prescription.getItems()) {
            inventoryService.reserve(prescription.getPharmacy().getId(), item.getMedicine().getId(),
                    item.getId(), item.getQuantityPrescribed(), validatedByUserId);
        }

        prescription.setStatus(PrescriptionStatus.VALIDATED);
        prescription.setValidatedBy(validator);
        prescription.setValidatedAt(Instant.now());
        prescription = prescriptionRepository.save(prescription);

        eventPublisher.publish(new PrescriptionValidatedEvent(prescription.getId(), prescription.getPrescriptionNumber(),
                prescription.getPharmacy().getId(), validatedByUserId, true, null, Instant.now()));

        return PrescriptionResponse.from(prescription);
    }

    @Override
    @Transactional
    public PrescriptionResponse reject(Long prescriptionId, String reason, Long validatedByUserId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Prescription", prescriptionId));

        if (prescription.getStatus() != PrescriptionStatus.CREATED) {
            throw new InvalidStateTransitionException("Only newly created prescriptions can be rejected");
        }

        User validator = userRepository.findById(validatedByUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", validatedByUserId));

        prescription.setStatus(PrescriptionStatus.REJECTED);
        prescription.setValidatedBy(validator);
        prescription.setValidatedAt(Instant.now());
        prescription.setRejectionReason(reason);
        prescription = prescriptionRepository.save(prescription);

        eventPublisher.publish(new PrescriptionValidatedEvent(prescription.getId(), prescription.getPrescriptionNumber(),
                prescription.getPharmacy().getId(), validatedByUserId, false, reason, Instant.now()));

        return PrescriptionResponse.from(prescription);
    }

    @Override
    @Transactional
    public PrescriptionResponse cancel(Long prescriptionId, Long performedByUserId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Prescription", prescriptionId));

        if (prescription.getStatus() == PrescriptionStatus.DISPENSED
                || prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Prescription cannot be cancelled from its current state");
        }

        if (prescription.getStatus() == PrescriptionStatus.VALIDATED
                || prescription.getStatus() == PrescriptionStatus.PARTIALLY_DISPENSED) {
            for (PrescriptionItem item : prescription.getItems()) {
                inventoryService.releaseReservationsForPrescriptionItem(item.getId(), performedByUserId);
            }
        }

        prescription.setStatus(PrescriptionStatus.CANCELLED);
        return PrescriptionResponse.from(prescriptionRepository.save(prescription));
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getById(Long id) {
        return prescriptionRepository.findById(id)
                .map(PrescriptionResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Prescription", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> findByPharmacy(Long pharmacyId, Pageable pageable) {
        return prescriptionRepository.findByPharmacyId(pharmacyId, pageable).map(PrescriptionResponse::from);
    }

    private String generatePrescriptionNumber() {
        return "RX-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
