package com.example.wallet.wallet.web;

import com.example.wallet.common.dto.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.Map;

/**
 * Protected wallet API. The acting user is taken from {@code jwt.getSubject()}
 * (the verified token), so a client can only ever see or move its own money —
 * there is no account id in any request path or body (anti-IDOR by design).
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final com.example.wallet.wallet.service.WalletService walletService;

    public WalletController(com.example.wallet.wallet.service.WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public BalanceResponse balance(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        return new BalanceResponse(username, walletService.getBalance(username));
    }

    @PostMapping("/transfer")
    public Map<String, String> transfer(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody TransferRequest request) {
        String username = jwt.getSubject();
        walletService.transfer(username, request.toUsername(), request.amount());
        return Map.of("status", "ok");
    }

    @GetMapping("/transactions")
    public List<TransactionView> transactions(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        return walletService.getTransactions(username).stream()
                .map(record -> TransactionView.forUser(record, username))
                .toList();
    }
}
