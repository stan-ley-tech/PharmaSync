package com.pharmasync.repository;

import com.pharmasync.domain.prescription.PrescriptionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);
}
