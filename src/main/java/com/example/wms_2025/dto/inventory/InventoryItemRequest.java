package com.example.wms_2025.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryItemRequest(
                @NotNull Long productId,
                @NotNull @Min(1) Integer quantity,
                Long locationId) {
}
