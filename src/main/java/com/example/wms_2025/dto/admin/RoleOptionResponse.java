package com.example.wms_2025.dto.admin;

import com.example.wms_2025.domain.enums.RoleCode;

public record RoleOptionResponse(RoleCode code, String name, String description) {
}
