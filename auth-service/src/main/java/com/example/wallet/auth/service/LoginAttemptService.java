package com.example.wallet.auth.service;

import com.example.wallet.auth.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tracks failed login attempts and applies account lockout.
 *
 * <p>Crucially these methods run in their <b>own</b> transaction
 * ({@code REQUIRES_NEW}). A failed login throws to return 401, which rolls back
 * the caller's transaction — but the failure counter must survive that rollback,
 * otherwise lockout could never accumulate. The new transaction commits the
 * increment independently.</p>
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    private final UserRepository users;

    public LoginAttemptService(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username) {
        users.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES));
                log.warn("Account '{}' locked after {} failed attempts", username, attempts);
            }
            users.save(user);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(String username) {
        users.findByUsername(username).ifPresent(user -> {
            if (user.getFailedAttempts() != 0 || user.getLockedUntil() != null) {
                user.setFailedAttempts(0);
                user.setLockedUntil(null);
                users.save(user);
            }
        });
    }
}
