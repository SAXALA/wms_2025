package com.example.wms_2025.dto.warehouse;

import jakarta.validation.constraints.NotNull;

public record WarehouseLocationStatusRequest(@NotNull Boolean active) {
}
