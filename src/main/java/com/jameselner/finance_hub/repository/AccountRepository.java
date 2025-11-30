package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Account;
import com.jameselner.finance_hub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user = :user AND a.isActive = true")
    BigDecimal getTotalBalanceByUser(@Param("user") User user);
}
