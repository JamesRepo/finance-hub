package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Account;
import com.jameselner.finance_hub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find all accounts for a user
     */
    List<Account> findByUser(User user);

    /**
     * Find active accounts for a user
     */
    List<Account> findByUserAndIsActiveTrue(User user);

    /**
     * Count accounts for a user
     */
    long countByUser(User user);

    /**
     * Get total balance for all active accounts
     */
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user = :user AND a.isActive = true")
    BigDecimal getTotalBalanceByUser(@Param("user") User user);
}
