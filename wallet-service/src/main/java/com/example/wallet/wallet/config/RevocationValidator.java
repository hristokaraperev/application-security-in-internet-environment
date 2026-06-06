package com.example.wallet.wallet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Rejects access tokens that the auth-service has revoked (e.g. on logout). For
 * every token it asks the auth-service's introspection endpoint whether the
 * token's {@code jti} is blocklisted. This is what makes "logout" actually stop
 * a token that is still within its validity window.
 *
 * <p>Fails open on network errors (treats the token as not revoked) so a
 * transient auth-service hiccup does not break all wallet access — the short
 * token lifetime remains the backstop.</p>
 */
public class RevocationValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger log = LoggerFactory.getLogger(RevocationValidator.class);

    private static final OAuth2Error REVOKED_ERROR =
            new OAuth2Error("token_revoked", "The access token has been revoked", null);

    private final RestClient restClient;

    public RevocationValidator(String authBaseUri) {
        this.restClient = RestClient.builder().baseUrl(authBaseUri).build();
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String subject = token.getSubject();
        Instant issuedAt = token.getIssuedAt();
        if (subject == null || issuedAt == null) {
            return OAuth2TokenValidatorResult.success();
        }
        try {
            Map<?, ?> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/auth/introspect")
                            .queryParam("sub", subject)
                            .queryParam("iat", issuedAt.getEpochSecond())
                            .build())
                    .retrieve()
                    .body(Map.class);
            if (body != null && Boolean.TRUE.equals(body.get("revoked"))) {
                return OAuth2TokenValidatorResult.failure(REVOKED_ERROR);
            }
        } catch (Exception e) {
            log.warn("Introspection call failed, allowing token (fail-open): {}", e.getMessage());
        }
        return OAuth2TokenValidatorResult.success();
    }
}
