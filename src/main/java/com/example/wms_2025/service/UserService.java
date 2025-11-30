package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.RoleCode;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.UserRepository;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getCurrentUser(RoleCode... preferredRoles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || isAnonymous(authentication)) {
            return resolveFallbackUser(preferredRoles);
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseGet(() -> resolveFallbackUser(preferredRoles));
    }

    public User getManager() {
        List<User> managers = userRepository.findByRolesRoleCode(RoleCode.MANAGER);
        return managers.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("Manager not configured"));
    }

    private boolean isAnonymous(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        return principal instanceof String && "anonymousUser".equals(principal);
    }

    private User resolveFallbackUser(RoleCode... preferredRoles) {
        Set<RoleCode> preferred = preferredRoles == null || preferredRoles.length == 0
                ? EnumSet.noneOf(RoleCode.class)
                : EnumSet.copyOf(Arrays.asList(preferredRoles));

        for (RoleCode role : preferred) {
            List<User> users = userRepository.findByRolesRoleCode(role);
            if (!users.isEmpty()) {
                return users.get(0);
            }
        }

        for (RoleCode role : List.of(RoleCode.PURCHASER, RoleCode.OPERATOR, RoleCode.MANAGER, RoleCode.ADMIN)) {
            if (preferred.contains(role)) {
                continue;
            }
            List<User> users = userRepository.findByRolesRoleCode(role);
            if (!users.isEmpty()) {
                return users.get(0);
            }
        }

        return userRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("User not found"));
    }
}
