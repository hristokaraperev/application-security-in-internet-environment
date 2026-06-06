package com.example.wallet.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects any request that does not carry a valid {@link GatewayToken}. Mounted
 * on the protected services so that direct calls (bypassing the gateway) are
 * refused with 401 before any business logic — or even JWT validation — runs.
 *
 * <p>This is the enforcement point for "validate the identity of the calling
 * service" from the assignment.</p>
 */
public class ServiceTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenFilter.class);

    private final String sharedSecret;
    private final long maxSkewSeconds;

    public ServiceTokenFilter(String sharedSecret, long maxSkewSeconds) {
        this.sharedSecret = sharedSecret;
        this.maxSkewSeconds = maxSkewSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health probes are called directly (e.g. by the container runtime),
        // not through the gateway, so they are exempt from the service token.
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String timestamp = request.getHeader(GatewayToken.HEADER_TIMESTAMP);
        String signature = request.getHeader(GatewayToken.HEADER_SIGNATURE);

        if (!GatewayToken.verify(sharedSecret, timestamp, signature, maxSkewSeconds)) {
            log.warn("Rejected request to {} {} — missing/invalid gateway token (direct call?)",
                    request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"forbidden_caller\",\"message\":\"Request must come through the API gateway\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
