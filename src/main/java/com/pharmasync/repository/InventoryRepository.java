package com.pharmasync.repository;

import com.pharmasync.domain.inventory.Inventory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByPharmacyIdAndMedicineId(Long pharmacyId, Long medicineId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.pharmacy.id = :pharmacyId AND i.medicine.id = :medicineId")
    Optional<Inventory> lockByPharmacyIdAndMedicineId(@Param("pharmacyId") Long pharmacyId,
                                                        @Param("medicineId") Long medicineId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> lockById(@Param("id") Long id);

    @Query("""
            SELECT i FROM Inventory i
            WHERE i.quantityOnHand - i.quantityReserved <=
                  COALESCE(i.reorderThreshold, i.medicine.reorderThreshold)
            """)
    List<Inventory> findAllBelowReorderThreshold();
}
