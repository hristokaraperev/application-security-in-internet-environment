package com.example.wallet.wallet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Append-only audit trail for money operations. Logs only metadata (who, to
 * whom, how much, outcome) — never tokens or other secrets — so the log itself
 * does not become a data-exposure risk.
 */
@Service
public class AuditService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    public void transferSucceeded(String from, String to, BigDecimal amount) {
        audit.info("TRANSFER_OK from={} to={} amount={}", from, to, amount);
    }

    public void transferRejected(String from, String to, BigDecimal amount, String reason) {
        audit.warn("TRANSFER_REJECTED from={} to={} amount={} reason={}", from, to, amount, reason);
    }
}
