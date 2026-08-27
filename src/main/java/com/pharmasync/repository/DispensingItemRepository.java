package com.pharmasync.repository;

import com.pharmasync.domain.dispensing.DispensingItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispensingItemRepository extends JpaRepository<DispensingItem, Long> {

    List<DispensingItem> findByDispensingId(Long dispensingId);
}
