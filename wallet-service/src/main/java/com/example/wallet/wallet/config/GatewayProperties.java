package com.example.wallet.wallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared secret used to verify the {@code X-Gateway-Auth} service token, bound
 * from {@code app.gateway.*}.
 */
@ConfigurationProperties(prefix = "app.gateway")
public record GatewayProperties(
        String sharedSecret,
        long maxSkewSeconds
) {
}
