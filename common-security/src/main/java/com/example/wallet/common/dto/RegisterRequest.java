package com.example.wallet.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Bean-validation constraints reject malformed input at
 * the edge, before it ever reaches persistence (defence against injection and
 * abuse via oversized / weird values).
 */
public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "username may contain only letters, digits, _ . -")
        String username,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
