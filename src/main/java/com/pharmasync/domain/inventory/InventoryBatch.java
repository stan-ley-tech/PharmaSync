package com.pharmasync.domain.inventory;

import com.pharmasync.common.BaseEntity;
import com.pharmasync.domain.procurement.PurchaseOrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_batches")
public class InventoryBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "batch_number", nullable = false, length = 60)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id")
    private PurchaseOrderItem purchaseOrderItem;

    @Column(name = "quantity_received", nullable = false)
    private int quantityReceived;

    @Column(name = "quantity_remaining", nullable = false)
    private int quantityRemaining;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "manufactured_date")
    private LocalDate manufacturedDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status = BatchStatus.ACTIVE;

    @Version
    @Column(nullable = false)
    private long version;

    public boolean isExpired(LocalDate asOf) {
        return !expiryDate.isAfter(asOf);
    }
}
