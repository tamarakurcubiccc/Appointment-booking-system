package com.booking.user.service;

import com.booking.user.entity.Role;
import com.booking.user.entity.User;
import com.booking.user.repository.UserRepository;
import com.booking.user.security.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminAccountInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;

    public AdminAccountInitializer(UserRepository repository,
                                   PasswordEncoder passwordEncoder,
                                   AdminProperties properties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.info("No initial admin configured (security.admin.email/password) - skipping");
            return;
        }

        String email = properties.email().toLowerCase();
        if (repository.existsByEmailIgnoreCase(email)) {
            log.debug("Initial admin {} already exists", email);
            return;
        }

        var admin = new User(email, passwordEncoder.encode(properties.password()),
                properties.fullName() == null ? "Administrator" : properties.fullName(),
                null, Role.ADMIN);
        admin.activate();

        repository.save(admin);
        log.warn("Created the initial admin account {} - change its password before exposing this system",
                email);
    }
}
