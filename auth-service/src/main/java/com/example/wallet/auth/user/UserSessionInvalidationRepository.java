package com.example.wallet.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionInvalidationRepository extends JpaRepository<UserSessionInvalidation, String> {
}
