package com.pharmasync.service;

public record DispenseAllocation(Long inventoryBatchId, String batchNumber, int quantity) {
}
