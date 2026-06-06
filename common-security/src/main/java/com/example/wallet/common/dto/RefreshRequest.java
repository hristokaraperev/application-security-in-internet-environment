package com.example.wallet.common.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @NotBlank
        String refreshToken
) {
}
