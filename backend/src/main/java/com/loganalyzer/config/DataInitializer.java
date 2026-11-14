package com.loganalyzer.config;

import com.loganalyzer.entity.User;
import com.loganalyzer.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@loganalyzer.io")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role("ROLE_ADMIN")
                    .build();
            userRepository.save(admin);
            log.info("Initialized default admin account: admin / password123");
        }

        if (!userRepository.existsByUsername("developer")) {
            User dev = User.builder()
                    .username("developer")
                    .email("dev@loganalyzer.io")
                    .passwordHash(passwordEncoder.encode("devpass123"))
                    .role("ROLE_USER")
                    .build();
            userRepository.save(dev);
            log.info("Initialized default developer account: developer / devpass123");
        }
    }
}
