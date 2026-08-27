package com.pharmasync.repository;

import com.pharmasync.domain.dispensing.Dispensing;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispensingRepository extends JpaRepository<Dispensing, Long> {

    Optional<Dispensing> findByDispensingNumber(String dispensingNumber);

    Page<Dispensing> findByPrescriptionId(Long prescriptionId, Pageable pageable);

    Page<Dispensing> findByPharmacyId(Long pharmacyId, Pageable pageable);
}
