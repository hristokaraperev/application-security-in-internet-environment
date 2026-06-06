package com.example.wallet.auth.service;

import com.example.wallet.auth.user.UserSessionInvalidation;
import com.example.wallet.auth.user.UserSessionInvalidationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Tracks, per user, the moment all their access tokens were invalidated (logout).
 * The wallet-service asks {@link #isRevoked(String, long)} via introspection for
 * every request, so logging out immediately stops every access token the user
 * holds — regardless of whether the token was presented at logout.
 */
@Service
public class SessionRevocationService {

    private static final Logger log = LoggerFactory.getLogger(SessionRevocationService.class);

    private final UserSessionInvalidationRepository repository;

    public SessionRevocationService(UserSessionInvalidationRepository repository) {
        this.repository = repository;
    }

    /** Marks "now" as the cutoff: every access token issued before this is dead. */
    @Transactional
    public void invalidateUser(String username) {
        UserSessionInvalidation entry = repository.findById(username)
                .orElseGet(() -> new UserSessionInvalidation(username, Instant.now()));
        entry.setInvalidatedAt(Instant.now());
        repository.save(entry);
        log.info("Invalidated all access tokens for '{}'", username);
    }

    /**
     * @param subject the token's subject (username)
     * @param issuedAtEpochSeconds the token's {@code iat}
     * @return true if the token was issued before the user's last logout
     */
    @Transactional(readOnly = true)
    public boolean isRevoked(String subject, long issuedAtEpochSeconds) {
        return repository.findById(subject)
                .map(inv -> issuedAtEpochSeconds < inv.getInvalidatedAt().getEpochSecond())
                .orElse(false);
    }
}
