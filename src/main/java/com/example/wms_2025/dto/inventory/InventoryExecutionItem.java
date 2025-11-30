package com.example.wms_2025.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryExecutionItem(
                @NotNull Long productId,
                @NotNull @Min(0) Integer actualQuantity,
                Long locationId) {
}
