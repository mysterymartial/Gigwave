package com.gigwave.application.users;

import com.gigwave.domain.users.User;
import com.gigwave.domain.users.UserRole;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seeds the default admin user if not present.
 * Email: gigwave@gmail.com, Password: GigWaveAdmin1
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AdminUserSeeder implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "gigwave@gmail.com";
    private static final String ADMIN_PHONE = "08000000000";
    private static final String ADMIN_PASSWORD = "GigWaveAdmin1";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        User admin = User.builder()
                .id(UUID.randomUUID())
                .email(ADMIN_EMAIL)
                .phone(ADMIN_PHONE)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("Default admin user created: {}", ADMIN_EMAIL);
    }
}
