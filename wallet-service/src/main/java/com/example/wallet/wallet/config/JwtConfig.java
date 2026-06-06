package com.example.wallet.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Builds the JWT decoder used by the resource server: signature + expiry are
 * validated against the auth-service JWKS, and an extra {@link RevocationValidator}
 * rejects tokens that have been revoked (logout).
 */
@Configuration
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.auth.base-uri}")
    private String authBaseUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withRevocation = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new RevocationValidator(authBaseUri));
        decoder.setJwtValidator(withRevocation);
        return decoder;
    }
}
