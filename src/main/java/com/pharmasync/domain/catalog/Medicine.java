package com.pharmasync.domain.catalog;

import com.pharmasync.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medicines")
public class Medicine extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "generic_name", length = 200)
    private String genericName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MedicineForm form;

    @Column(length = 50)
    private String strength;

    @Column(length = 150)
    private String manufacturer;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "UNIT";

    @Column(name = "requires_prescription", nullable = false)
    private boolean requiresPrescription = true;

    @Column(name = "controlled_substance", nullable = false)
    private boolean controlledSubstance = false;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold = 20;

    @Column(name = "reorder_quantity", nullable = false)
    private int reorderQuantity = 100;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_supplier_id")
    private Supplier defaultSupplier;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
