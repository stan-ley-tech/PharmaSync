package com.pharmasync.web.dto;

import com.pharmasync.domain.inventory.Inventory;

public record InventoryResponse(
        Long id,
        Long pharmacyId,
        Long medicineId,
        String medicineName,
        int quantityOnHand,
        int quantityReserved,
        int quantityAvailable,
        int reorderThreshold) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getPharmacy().getId(),
                inventory.getMedicine().getId(),
                inventory.getMedicine().getName(),
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getQuantityAvailable(),
                inventory.effectiveReorderThreshold());
    }
}
