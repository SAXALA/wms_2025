package com.example.wms_2025.dto.inventory;

public record InventoryItemResponse(
                Long id,
                Long productId,
                Integer quantity,
                Integer actualQuantity,
                Long locationId,
                String locationCode,
                String locationName) {
}
