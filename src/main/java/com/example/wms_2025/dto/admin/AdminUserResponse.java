package com.example.wms_2025.dto.admin;

import com.example.wms_2025.domain.enums.RoleCode;
import com.example.wms_2025.domain.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.Set;

public record AdminUserResponse(Long id, String username, String realName, String department,
        UserStatus status, Set<RoleCode> roles, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
