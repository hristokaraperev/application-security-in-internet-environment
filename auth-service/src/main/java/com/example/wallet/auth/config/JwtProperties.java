package com.example.wallet.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token lifetimes and issuer id, bound from {@code app.jwt.*}. Short access /
 * long refresh is the core of the token-theft mitigation.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds
) {
}
