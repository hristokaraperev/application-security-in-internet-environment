package com.example.wallet.auth.config;

import com.example.wallet.auth.user.UserAccount;
import com.example.wallet.auth.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds two demo users (alice, bob) so the exam walkthrough — login, transfer,
 * IDOR attempt — works immediately with no manual setup. Their wallet balances
 * are seeded separately in the wallet-service.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEMO_PASSWORD = "Password123";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seed("alice");
        seed("bob");
    }

    private void seed(String username) {
        if (!users.existsByUsername(username)) {
            users.save(new UserAccount(username, passwordEncoder.encode(DEMO_PASSWORD)));
            log.info("Seeded demo user '{}' (password '{}')", username, DEMO_PASSWORD);
        }
    }
}
