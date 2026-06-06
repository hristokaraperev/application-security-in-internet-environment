package com.example.wallet.wallet.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletAccount, Long> {

    Optional<WalletAccount> findByUsername(String username);
}
