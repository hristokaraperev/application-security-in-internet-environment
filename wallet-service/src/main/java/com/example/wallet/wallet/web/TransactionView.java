package com.example.wallet.wallet.web;

import com.example.wallet.wallet.domain.TransactionRecord;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(
        String counterparty,
        String direction,
        BigDecimal amount,
        Instant createdAt
) {
    /** Renders a record from the perspective of the given user. */
    public static TransactionView forUser(TransactionRecord record, String username) {
        boolean outgoing = record.getFromUsername().equals(username);
        return new TransactionView(
                outgoing ? record.getToUsername() : record.getFromUsername(),
                outgoing ? "DEBIT" : "CREDIT",
                record.getAmount(),
                record.getCreatedAt());
    }
}
