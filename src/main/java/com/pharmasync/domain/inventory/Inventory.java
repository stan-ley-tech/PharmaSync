package com.pharmasync.domain.inventory;

import com.pharmasync.common.BaseEntity;
import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.pharmacy.Pharmacy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory")
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand = 0;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved = 0;

    @Column(name = "reorder_threshold")
    private Integer reorderThreshold;

    @Version
    @Column(nullable = false)
    private long version;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public int getQuantityAvailable() {
        return quantityOnHand - quantityReserved;
    }

    public int effectiveReorderThreshold() {
        return reorderThreshold != null ? reorderThreshold : medicine.getReorderThreshold();
    }
}
