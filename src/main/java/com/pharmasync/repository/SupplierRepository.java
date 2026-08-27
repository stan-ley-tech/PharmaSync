package com.pharmasync.repository;

import com.pharmasync.domain.catalog.Supplier;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByCode(String code);

    boolean existsByCode(String code);
}
