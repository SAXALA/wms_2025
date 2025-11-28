package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.Role;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.RoleCode;
import com.example.wms_2025.domain.enums.UserStatus;
import com.example.wms_2025.dto.admin.AdminUserCreateRequest;
import com.example.wms_2025.dto.admin.AdminUserResponse;
import com.example.wms_2025.dto.admin.AdminUserUpdateRequest;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.RoleRepository;
import com.example.wms_2025.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private static final String MODULE_USER_MANAGEMENT = "USER_MANAGEMENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;
    private final UserService userService;

    public Page<AdminUserResponse> searchUsers(String username, UserStatus status, RoleCode roleCode,
            Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        Specification<User> spec = Specification.where(null);
        if (StringUtils.hasText(username)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("username")),
                    "%" + username.toLowerCase() + "%"));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (roleCode != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("roles").get("roleCode"), roleCode));
        }
        return userRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(u -> {
            throw new BusinessException("Username already exists");
        });
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setDepartment(request.department());
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new HashSet<>(resolveRoles(request.roles())));
        User saved = userRepository.save(user);
        operationLogService.record(MODULE_USER_MANAGEMENT, "CREATE_USER",
                "Created user: " + saved.getUsername());
        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request) {
        User user = getUserOrThrow(id);
        user.setRealName(request.realName());
        user.setDepartment(request.department());
        operationLogService.record(MODULE_USER_MANAGEMENT, "UPDATE_USER",
                "Updated user: " + user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        ensureNotSelf(id, "delete the current administrator");
        if (operationLogService.hasLogs(id)) {
            throw new BusinessException("Cannot delete user with existing operation logs");
        }
        User user = getUserOrThrow(id);
        String username = user.getUsername();
        userRepository.delete(user);
        operationLogService.record(MODULE_USER_MANAGEMENT, "DELETE_USER",
                "Deleted user: " + username);
    }

    @Transactional
    public AdminUserResponse updateStatus(Long id, UserStatus status) {
        User user = getUserOrThrow(id);
        if (isCurrentUser(id) && status != UserStatus.ACTIVE) {
            throw new BusinessException("Cannot change status of the current administrator");
        }
        user.setStatus(status);
        operationLogService.record(MODULE_USER_MANAGEMENT, "UPDATE_STATUS",
                "Updated status for user: " + user.getUsername() + " -> " + status);
        return toResponse(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = getUserOrThrow(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        operationLogService.record(MODULE_USER_MANAGEMENT, "RESET_PASSWORD",
                "Reset password for user: " + user.getUsername());
    }

    @Transactional
    public AdminUserResponse assignRoles(Long id, Set<RoleCode> roleCodes) {
        User user = getUserOrThrow(id);
        Set<Role> roles = resolveRoles(roleCodes);
        boolean assigningAdmin = roles.stream().map(Role::getRoleCode).anyMatch(code -> code == RoleCode.ADMIN);
        if (isCurrentUser(id) && !assigningAdmin) {
            throw new BusinessException("Cannot remove ADMIN role from yourself");
        }
        user.setRoles(roles);
        operationLogService.record(MODULE_USER_MANAGEMENT, "ASSIGN_ROLES",
                "Updated roles for user: " + user.getUsername());
        return toResponse(user);
    }

    private Set<Role> resolveRoles(Set<RoleCode> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException("At least one role must be assigned");
        }
        List<Role> roles = roleRepository.findByRoleCodeIn(roleCodes);
        Set<RoleCode> found = roles.stream().map(Role::getRoleCode).collect(Collectors.toSet());
        roleCodes.stream().filter(code -> !found.contains(code)).findFirst()
                .ifPresent(code -> {
                    throw new BusinessException("Role not found: " + code);
                });
        return new HashSet<>(roles);
    }

    private User getUserOrThrow(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private void ensureNotSelf(Long targetId, String action) {
        if (isCurrentUser(targetId)) {
            throw new BusinessException("Cannot " + action);
        }
    }

    private boolean isCurrentUser(Long targetId) {
        User current = userService.getCurrentUser();
        return current.getId().equals(targetId);
    }

    private AdminUserResponse toResponse(User user) {
        Set<RoleCode> roleCodes = user.getRoles().stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toSet());
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getRealName(), user.getDepartment(),
                user.getStatus(), roleCodes, user.getCreatedAt(), user.getUpdatedAt());
    }
}
