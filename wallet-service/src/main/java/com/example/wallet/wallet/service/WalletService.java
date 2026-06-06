package com.example.wallet.wallet.service;

import com.example.wallet.wallet.domain.TransactionRecord;
import com.example.wallet.wallet.domain.TransactionRepository;
import com.example.wallet.wallet.domain.WalletAccount;
import com.example.wallet.wallet.domain.WalletRepository;
import com.example.wallet.wallet.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wallet business logic. Every method takes the acting username from the
 * authenticated token (passed in by the controller) — never from client input —
 * which is the structural defence against IDOR / broken access control.
 */
@Service
public class WalletService {

    private final WalletRepository wallets;
    private final TransactionRepository transactions;
    private final AuditService audit;

    public WalletService(WalletRepository wallets, TransactionRepository transactions, AuditService audit) {
        this.wallets = wallets;
        this.transactions = transactions;
        this.audit = audit;
    }

    @Transactional
    public BigDecimal getBalance(String username) {
        return getOrCreateAccount(username).getBalance();
    }

    @Transactional(readOnly = true)
    public List<TransactionRecord> getTransactions(String username) {
        return transactions.findForUser(username);
    }

    @Transactional
    public void transfer(String fromUsername, String toUsername, BigDecimal amount) {
        // Defence in depth: re-check the amount server-side even though the DTO
        // already validated it (never trust the client).
        if (amount.signum() <= 0) {
            audit.transferRejected(fromUsername, toUsername, amount, "non_positive_amount");
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_amount", "Amount must be positive");
        }
        if (fromUsername.equals(toUsername)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "self_transfer", "Cannot transfer to yourself");
        }

        WalletAccount sender = getOrCreateAccount(fromUsername);
        WalletAccount recipient = wallets.findByUsername(toUsername)
                .orElseThrow(() -> {
                    audit.transferRejected(fromUsername, toUsername, amount, "recipient_not_found");
                    return new ApiException(HttpStatus.NOT_FOUND, "recipient_not_found", "Recipient does not exist");
                });

        if (sender.getBalance().compareTo(amount) < 0) {
            audit.transferRejected(fromUsername, toUsername, amount, "insufficient_funds");
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "insufficient_funds", "Insufficient funds");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        recipient.setBalance(recipient.getBalance().add(amount));
        transactions.save(new TransactionRecord(fromUsername, toUsername, amount));
        audit.transferSucceeded(fromUsername, toUsername, amount);
    }

    /** Lazily creates a zero-balance wallet so any authenticated user has one. */
    private WalletAccount getOrCreateAccount(String username) {
        return wallets.findByUsername(username)
                .orElseGet(() -> wallets.save(new WalletAccount(username, BigDecimal.ZERO)));
    }
}
