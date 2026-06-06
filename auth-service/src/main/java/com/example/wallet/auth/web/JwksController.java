package com.example.wallet.auth.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publishes the public half of the signing key as a JWK Set. The wallet-service
 * fetches this to verify access tokens — the public key can be shared freely,
 * the private key never leaves this service.
 */
@RestController
public class JwksController {

    private final RSAKey rsaKey;

    public JwksController(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        // toPublicJWK() strips the private key material.
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
