package com.example.wallet.wallet.web;

import java.math.BigDecimal;

public record BalanceResponse(String username, BigDecimal balance) {
}
