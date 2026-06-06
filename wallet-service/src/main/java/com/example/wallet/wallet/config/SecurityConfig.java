package com.example.wallet.wallet.config;

import com.example.wallet.common.ServiceTokenFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Wallet-service is an OAuth2 resource server. Two gates protect every
 * {@code /api/**} call:
 * <ol>
 *   <li>{@link ServiceTokenFilter} — proves the request came through the gateway
 *       (service-to-service auth), runs first so direct calls fail fast;</li>
 *   <li>JWT bearer authentication — verifies the user's RS256 token against the
 *       auth-service JWKS.</li>
 * </ol>
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, GatewayProperties gateway) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .addFilterBefore(
                        new ServiceTokenFilter(gateway.sharedSecret(), gateway.maxSkewSeconds()),
                        BearerTokenAuthenticationFilter.class)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny()));
        return http.build();
    }
}
