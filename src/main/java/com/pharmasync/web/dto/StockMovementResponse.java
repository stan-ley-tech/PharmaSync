package com.pharmasync.web.dto;

import com.pharmasync.domain.inventory.MovementType;
import com.pharmasync.domain.inventory.StockMovement;
import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long inventoryBatchId,
        MovementType movementType,
        int quantity,
        int quantityBefore,
        int quantityAfter,
        String referenceType,
        Long referenceId,
        Instant createdAt) {

    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getInventoryBatch().getId(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getQuantityBefore(),
                movement.getQuantityAfter(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getCreatedAt());
    }
}
