package com.uniform.store.bootstrap;

import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.repository.RoleRepository;
import com.uniform.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final String DEFAULT_ADMIN_EMAIL = "longpd1911@gmail.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "longan47";
    private static final String DEFAULT_ADMIN_NAME = "Long Pham";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            log.info("Admin bootstrap skipped: {} already exists", DEFAULT_ADMIN_EMAIL);
            return;
        }
        Role adminRole = roleRepository.findByName(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Admin role missing; V1 migration must run first"));

        userRepository.save(User.builder()
                .role(adminRole)
                .email(DEFAULT_ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .fullName(DEFAULT_ADMIN_NAME)
                .preferredLocale("vi")
                .status(UserStatus.ACTIVE)
                .build());
        log.info("Admin bootstrap created default admin: {}", DEFAULT_ADMIN_EMAIL);
    }
}
