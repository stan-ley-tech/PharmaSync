package com.pharmasync.domain.dispensing;

import com.pharmasync.common.BaseEntity;
import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.domain.prescription.Prescription;
import com.pharmasync.domain.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dispensing")
public class Dispensing extends BaseEntity {

    @Column(name = "dispensing_number", nullable = false, unique = true, length = 40)
    private String dispensingNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispensed_by", nullable = false)
    private User dispensedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispensingStatus status = DispensingStatus.COMPLETED;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "dispensed_at", nullable = false)
    private Instant dispensedAt = Instant.now();

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "dispensing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DispensingItem> items = new ArrayList<>();

    public void addItem(DispensingItem item) {
        items.add(item);
        item.setDispensing(this);
    }
}
