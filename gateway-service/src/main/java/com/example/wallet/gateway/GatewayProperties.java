package com.example.wallet.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gateway")
public record GatewayProperties(
        String sharedSecret,
        int rateLimitRequests,
        int rateLimitWindowSeconds
) {
}
