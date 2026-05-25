package com.uniform.store.integration;

import com.uniform.store.bootstrap.AdminBootstrap;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class AdminBootstrapIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "longpd1911@gmail.com";
    private static final String PASSWORD = "longan47";

    @Autowired private AdminBootstrap adminBootstrap;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void createsDefaultAdminWhenAbsent() {
        adminBootstrap.run();

        User admin = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(admin.getRole().getName()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches(PASSWORD, admin.getPasswordHash())).isTrue();
    }

    @Test
    void isIdempotentOnSecondRun() {
        adminBootstrap.run();
        long after1 = userRepository.count();

        adminBootstrap.run();
        long after2 = userRepository.count();

        assertThat(after1).isEqualTo(after2);
    }
}
