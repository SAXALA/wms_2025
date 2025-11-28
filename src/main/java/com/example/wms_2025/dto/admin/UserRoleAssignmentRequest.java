package com.example.wms_2025.dto.admin;

import com.example.wms_2025.domain.enums.RoleCode;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserRoleAssignmentRequest(@NotEmpty Set<RoleCode> roleCodes) {
}
