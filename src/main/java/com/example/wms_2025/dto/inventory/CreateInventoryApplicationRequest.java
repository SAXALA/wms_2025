package com.example.wms_2025.dto.inventory;

import com.example.wms_2025.domain.enums.InventoryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateInventoryApplicationRequest(
        @NotNull InventoryType type,
        @NotBlank String reason,
        @Valid @Size(min = 1) List<InventoryItemRequest> items) {
}
