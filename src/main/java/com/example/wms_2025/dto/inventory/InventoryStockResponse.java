package com.example.wms_2025.dto.inventory;

public record InventoryStockResponse(Long productId,
        String sku,
        String name,
        String unit,
        Integer currentStock,
        Integer safetyStock,
        Integer lockedStock,
        Long locationId,
        String locationCode,
        String locationName) {
}
