package com.example.wallet.common.dto;

import java.time.Instant;

/**
 * Uniform error body. Deliberately carries only a short code and a safe message
 * — never stack traces or internal details (defence against information
 * leakage / insecure error handling).
 */
public record ApiError(
        String error,
        String message,
        Instant timestamp
) {
    public static ApiError of(String error, String message) {
        return new ApiError(error, message, Instant.now());
    }
}
