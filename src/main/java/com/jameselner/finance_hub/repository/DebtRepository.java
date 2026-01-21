package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    @Query("SELECT COALESCE(SUM(d.currentBalance), 0) FROM Debt d WHERE d.user = :user AND d.active = true")
    BigDecimal getTotalDebtByUser(@Param("user") User user);
}
