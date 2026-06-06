package com.example.wallet.wallet.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

    /** All transactions where the user is sender or receiver, newest first. */
    @Query("select t from TransactionRecord t where t.fromUsername = :username or t.toUsername = :username "
            + "order by t.createdAt desc")
    List<TransactionRecord> findForUser(@Param("username") String username);
}
