package com.example.wallet.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.username = :username and t.revoked = false")
    int revokeAllForUser(@Param("username") String username);
}
