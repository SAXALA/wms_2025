package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.RoleCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    List<User> findByRolesRoleCode(RoleCode roleCode);
}
