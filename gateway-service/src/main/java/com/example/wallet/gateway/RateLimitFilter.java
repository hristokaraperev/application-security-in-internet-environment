package com.example.wallet.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A small in-memory fixed-window rate limiter per client IP. Defence-in-depth at
 * the edge against floods and brute-force bursts — complements the per-account
 * lockout in the auth-service. Kept dependency-free (no Redis) on purpose for a
 * single-instance demo.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final int maxRequests;
    private final long windowSeconds;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(GatewayProperties properties) {
        this.maxRequests = properties.rateLimitRequests();
        this.windowSeconds = properties.rateLimitWindowSeconds();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isOverLimit(clientKey(exchange))) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private boolean isOverLimit(String key) {
        long currentWindow = System.currentTimeMillis() / 1000 / windowSeconds;
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.windowId != currentWindow) {
                return new Window(currentWindow);
            }
            return existing;
        });
        return window.count.incrementAndGet() > maxRequests;
    }

    private String clientKey(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        // Run first, before any routing/proxying work.
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static final class Window {
        private final long windowId;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(long windowId) {
            this.windowId = windowId;
        }
    }
}
