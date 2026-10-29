package com.complymind.config;

import com.complymind.user.entity.User;
import com.complymind.user.entity.UserRole;
import com.complymind.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("dev")
public class DefaultUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Ensure default 'admin' account exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@complymind.ai");
            admin.setFullName("System Administrator");
            admin.setRole(UserRole.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            userRepository.save(admin);
            log.info("Default administrator account created: username=admin, password=Admin@123");
        }

        // Ensure user 'selvam' has known password 'Admin@123' as well as 'NewPassword123!'
        userRepository.findByUsername("selvam").ifPresent(selvam -> {
            selvam.setPasswordHash(passwordEncoder.encode("Admin@123"));
            selvam.setEnabled(true);
            selvam.setUpdatedAt(LocalDateTime.now());
            userRepository.save(selvam);
            log.info("User 'selvam' password synchronized to 'Admin@123'");
        });
    }
}
