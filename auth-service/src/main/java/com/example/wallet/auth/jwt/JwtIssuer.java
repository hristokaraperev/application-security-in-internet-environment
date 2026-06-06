package com.example.wallet.auth.jwt;

import com.example.wallet.auth.config.JwtProperties;
import com.example.wallet.common.JwtClaims;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Mints short-lived RS256 access tokens. The wallet-service verifies them using
 * the public key published at the JWKS endpoint — no shared secret required.
 */
@Component
public class JwtIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public JwtIssuer(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public IssuedToken issueAccessToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(props.accessTokenTtlSeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim(JwtClaims.TOKEN_TYPE, JwtClaims.TYPE_ACCESS)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, props.accessTokenTtlSeconds());
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
