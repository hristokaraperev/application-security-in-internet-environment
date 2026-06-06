package com.example.wallet.auth.web;

import com.example.wallet.auth.service.SessionRevocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Minimal token introspection. The wallet-service passes a token's subject and
 * issued-at; the auth-service answers whether that token has been revoked (i.e.
 * issued before the user's last logout). This is what turns the stateless access
 * tokens into revocable sessions.
 */
@RestController
public class IntrospectionController {

    private final SessionRevocationService sessionRevocation;

    public IntrospectionController(SessionRevocationService sessionRevocation) {
        this.sessionRevocation = sessionRevocation;
    }

    @GetMapping("/auth/introspect")
    public Map<String, Boolean> introspect(@RequestParam String sub, @RequestParam long iat) {
        return Map.of("revoked", sessionRevocation.isRevoked(sub, iat));
    }
}
