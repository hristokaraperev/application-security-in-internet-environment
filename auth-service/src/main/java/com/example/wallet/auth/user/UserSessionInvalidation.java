package com.example.wallet.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Per-user "logout watermark". Any access token whose {@code iat} (issued-at) is
 * before {@code invalidatedAt} is considered revoked.
 *
 * <p>This is keyed by username — not by token — so a logout invalidates <b>all</b>
 * of the user's currently-issued access tokens at once, without the client having
 * to present the access token. That is what makes logout actually stop a stolen
 * or replayed token.</p>
 */
@Entity
@Table(name = "user_session_invalidations")
public class UserSessionInvalidation {

    @Id
    @Column(length = 32)
    private String username;

    @Column(nullable = false)
    private Instant invalidatedAt;

    protected UserSessionInvalidation() {
    }

    public UserSessionInvalidation(String username, Instant invalidatedAt) {
        this.username = username;
        this.invalidatedAt = invalidatedAt;
    }

    public String getUsername() {
        return username;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(Instant invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
    }
}
