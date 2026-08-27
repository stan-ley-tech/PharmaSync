package com.pharmasync.domain.prescription;

import com.pharmasync.common.BaseEntity;
import com.pharmasync.domain.catalog.Medicine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(name = "quantity_prescribed", nullable = false)
    private int quantityPrescribed;

    @Column(name = "quantity_dispensed", nullable = false)
    private int quantityDispensed = 0;

    @Column(name = "dosage_instructions", length = 300)
    private String dosageInstructions;

    @Column(name = "substitution_allowed", nullable = false)
    private boolean substitutionAllowed = true;

    public int getQuantityOutstanding() {
        return quantityPrescribed - quantityDispensed;
    }
}
