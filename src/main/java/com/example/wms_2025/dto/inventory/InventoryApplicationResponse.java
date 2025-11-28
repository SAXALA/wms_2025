package com.example.wms_2025.dto.inventory;

import com.example.wms_2025.domain.enums.InventoryApplicationStatus;
import com.example.wms_2025.domain.enums.InventoryType;
import java.time.LocalDateTime;
import java.util.List;

public record InventoryApplicationResponse(
        Long id,
        InventoryType type,
        InventoryApplicationStatus status,
        String applicant,
        String reason,
        LocalDateTime createdAt,
        List<InventoryItemResponse> items) {
}
