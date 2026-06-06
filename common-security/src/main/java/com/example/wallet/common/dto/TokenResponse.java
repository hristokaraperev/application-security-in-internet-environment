package com.example.wallet.common.dto;

/**
 * Issued at login/refresh. The access token is short-lived and sent on every
 * API call; the refresh token is long-lived, rotated on use, and revocable.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
