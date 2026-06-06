package com.example.wallet.auth.web;

import com.example.wallet.auth.service.AuthService;
import com.example.wallet.common.dto.LoginRequest;
import com.example.wallet.common.dto.RefreshRequest;
import com.example.wallet.common.dto.RegisterRequest;
import com.example.wallet.common.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "registered"));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshRequest request) {
        // Revokes the refresh token AND invalidates all of the user's access tokens
        // server-side — no need for the client to present the access token.
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }
}
