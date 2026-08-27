package com.pharmasync.repository;

import com.pharmasync.domain.pharmacy.Pharmacy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    Optional<Pharmacy> findByCode(String code);

    boolean existsByCode(String code);
}
