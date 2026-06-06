package com.example.wallet.wallet.config;

import com.example.wallet.wallet.domain.WalletAccount;
import com.example.wallet.wallet.domain.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds starting balances for the demo users so a transfer (alice → bob) and an
 * IDOR attempt can be shown immediately. Usernames must match those seeded in
 * the auth-service.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final WalletRepository wallets;

    public DataInitializer(WalletRepository wallets) {
        this.wallets = wallets;
    }

    @Override
    public void run(String... args) {
        seed("alice", new BigDecimal("1000.00"));
        seed("bob", new BigDecimal("500.00"));
    }

    private void seed(String username, BigDecimal balance) {
        if (wallets.findByUsername(username).isEmpty()) {
            wallets.save(new WalletAccount(username, balance));
            log.info("Seeded wallet for '{}' with balance {}", username, balance);
        }
    }
}
