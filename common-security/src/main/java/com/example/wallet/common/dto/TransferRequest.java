package com.example.wallet.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Money transfer payload. The amount is a {@link BigDecimal} validated to be
 * strictly positive and bounded — server-side validation defends against amount
 * manipulation (e.g. negative transfers that would credit the attacker).
 */
public record TransferRequest(

        @NotBlank
        @Size(max = 32)
        String toUsername,

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be positive")
        @Digits(integer = 12, fraction = 2, message = "amount has at most 2 decimal places")
        BigDecimal amount
) {
}
