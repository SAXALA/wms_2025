package com.example.wms_2025.config;

import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserPasswordInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserPasswordInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        boolean updated = false;
        for (User user : users) {
            if (needsEncoding(user.getPassword())) {
                String original = user.getPassword();
                user.setPassword(passwordEncoder.encode(original));
                updated = true;
                log.info("Initialized password for user {}", user.getUsername());
            }
        }
        if (updated) {
            userRepository.flush();
        }
    }

    private boolean needsEncoding(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        String trimmed = password.trim();
        if (trimmed.startsWith("$2a$") || trimmed.startsWith("$2b$") || trimmed.startsWith("$2y$")) {
            return false;
        }
        return true;
    }
}
