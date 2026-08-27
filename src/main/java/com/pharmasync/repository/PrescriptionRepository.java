package com.pharmasync.repository;

import com.pharmasync.domain.prescription.Prescription;
import com.pharmasync.domain.prescription.PrescriptionStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByPrescriptionNumber(String prescriptionNumber);

    Page<Prescription> findByPharmacyIdAndStatus(Long pharmacyId, PrescriptionStatus status, Pageable pageable);

    Page<Prescription> findByPharmacyId(Long pharmacyId, Pageable pageable);
}
