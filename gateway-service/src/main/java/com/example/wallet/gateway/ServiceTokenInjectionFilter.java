package com.example.wallet.gateway;

import com.example.wallet.common.GatewayToken;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Stamps every proxied request with a fresh {@link GatewayToken} so the
 * downstream services can prove the call really came through the gateway. This
 * is the gateway side of the service-to-service authentication.
 */
@Component
public class ServiceTokenInjectionFilter implements GlobalFilter, Ordered {

    private final GatewayProperties properties;

    public ServiceTokenInjectionFilter(GatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String[] signed = GatewayToken.sign(properties.sharedSecret());
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(GatewayToken.HEADER_TIMESTAMP, signed[0])
                .header(GatewayToken.HEADER_SIGNATURE, signed[1])
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        // Must run before the NettyRoutingFilter (LOWEST_PRECEDENCE) so the
        // injected headers are present when the request is proxied downstream.
        return 0;
    }
}
