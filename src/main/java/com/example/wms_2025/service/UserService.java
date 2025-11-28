package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.RoleCode;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("Unauthenticated user");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    public User getManager() {
        List<User> managers = userRepository.findByRolesRoleCode(RoleCode.MANAGER);
        return managers.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("Manager not configured"));
    }
}
