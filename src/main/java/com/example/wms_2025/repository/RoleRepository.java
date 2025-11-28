package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.Role;
import com.example.wms_2025.domain.enums.RoleCode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleCode(RoleCode roleCode);

    List<Role> findByRoleCodeIn(Collection<RoleCode> roleCodes);
}
