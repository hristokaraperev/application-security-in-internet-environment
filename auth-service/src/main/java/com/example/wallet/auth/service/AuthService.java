package com.example.wallet.auth.service;

import com.example.wallet.auth.config.JwtProperties;
import com.example.wallet.auth.jwt.JwtIssuer;
import com.example.wallet.auth.user.RefreshToken;
import com.example.wallet.auth.user.RefreshTokenRepository;
import com.example.wallet.auth.user.UserAccount;
import com.example.wallet.auth.user.UserRepository;
import com.example.wallet.auth.web.ApiException;
import com.example.wallet.common.dto.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Core authentication logic: registration, credential verification with lockout,
 * and the full refresh-token lifecycle (issue, rotate, revoke).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;
    private final JwtProperties jwtProperties;
    private final LoginAttemptService loginAttempts;
    private final SessionRevocationService sessionRevocation;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, JwtIssuer jwtIssuer, JwtProperties jwtProperties,
                       LoginAttemptService loginAttempts, SessionRevocationService sessionRevocation) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
        this.jwtProperties = jwtProperties;
        this.loginAttempts = loginAttempts;
        this.sessionRevocation = sessionRevocation;
    }

    @Transactional
    public void register(String username, String rawPassword) {
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "username_taken", "Username is already taken");
        }
        users.save(new UserAccount(username, passwordEncoder.encode(rawPassword)));
        log.info("Registered new user '{}'", username);
    }

    @Transactional
    public TokenResponse login(String username, String rawPassword) {
        UserAccount user = users.findByUsername(username).orElse(null);

        // Uniform failure for unknown users — avoids username enumeration.
        if (user == null) {
            throw invalidCredentials();
        }
        if (isLocked(user)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "account_locked",
                    "Too many failed attempts. Try again later.");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            // Persisted in its own transaction so it survives the rollback from the throw below.
            loginAttempts.recordFailure(username);
            throw invalidCredentials();
        }

        // Success — clear any accumulated failures and issue tokens.
        loginAttempts.reset(username);
        return issueTokens(username);
    }

    @Transactional
    public TokenResponse refresh(String presentedRefreshToken) {
        RefreshToken token = refreshTokens.findById(presentedRefreshToken).orElse(null);
        if (token == null || !token.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_refresh", "Refresh token is invalid or expired");
        }
        // Rotation: the presented token is single-use.
        token.setRevoked(true);
        return issueTokens(token.getUsername());
    }

    @Transactional
    public void logout(String presentedRefreshToken) {
        refreshTokens.findById(presentedRefreshToken).ifPresent(token -> {
            token.setRevoked(true);
            // Revoke every active refresh session for this user.
            refreshTokens.revokeAllForUser(token.getUsername());
            // Invalidate every already-issued access token for this user too — this
            // does NOT require the access token to be presented at logout.
            sessionRevocation.invalidateUser(token.getUsername());
            log.info("Logged out user '{}'", token.getUsername());
        });
    }

    private TokenResponse issueTokens(String username) {
        JwtIssuer.IssuedToken access = jwtIssuer.issueAccessToken(username);
        String refreshId = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(jwtProperties.refreshTokenTtlSeconds());
        refreshTokens.save(new RefreshToken(refreshId, username, expiry));
        return TokenResponse.bearer(access.value(), refreshId, access.expiresInSeconds());
    }

    private boolean isLocked(UserAccount user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid username or password");
    }
}
