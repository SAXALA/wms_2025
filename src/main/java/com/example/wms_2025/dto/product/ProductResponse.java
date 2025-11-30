package com.example.wms_2025.dto.product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String sku, String unit, BigDecimal price) {
}
