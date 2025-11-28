package com.example.wms_2025.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ExecuteInventoryRequest(@Valid @Size(min = 1) List<InventoryExecutionItem> items) {
}
