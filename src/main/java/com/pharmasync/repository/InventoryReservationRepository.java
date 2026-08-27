package com.pharmasync.repository;

import com.pharmasync.domain.inventory.InventoryReservation;
import com.pharmasync.domain.inventory.ReservationStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findByPrescriptionItemIdAndStatus(Long prescriptionItemId, ReservationStatus status);

    List<InventoryReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);
}
