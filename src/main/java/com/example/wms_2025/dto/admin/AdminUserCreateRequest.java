package com.example.wms_2025.dto.admin;

import com.example.wms_2025.domain.enums.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AdminUserCreateRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 64) String realName,
        @Size(max = 64) String department,
        @NotEmpty Set<RoleCode> roles) {
}
