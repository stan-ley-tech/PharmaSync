package com.pharmasync.repository;

import com.pharmasync.domain.catalog.Medicine;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("""
            SELECT m FROM Medicine m
            WHERE m.active = true
              AND (:search IS NULL OR lower(m.name) LIKE lower(concat('%', :search, '%'))
                   OR lower(m.genericName) LIKE lower(concat('%', :search, '%')))
            """)
    Page<Medicine> search(@Param("search") String search, Pageable pageable);
}
