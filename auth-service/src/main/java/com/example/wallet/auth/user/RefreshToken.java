package com.example.wallet.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A persisted refresh token. Storing them server-side is what makes refresh
 * tokens revocable (logout) and rotatable (each refresh marks the old one used
 * and issues a new id). The primary key is the token's {@code jti}.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 32)
    private String username;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected RefreshToken() {
    }

    public RefreshToken(String id, String username, Instant expiresAt) {
        this.id = id;
        this.username = username;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isActive() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }
}
