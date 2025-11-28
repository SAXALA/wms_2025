package com.example.wms_2025.dto.inventory;

import jakarta.validation.constraints.NotNull;

public record ApproveInventoryRequest(
        @NotNull Boolean approved,
        String comment) {
}
