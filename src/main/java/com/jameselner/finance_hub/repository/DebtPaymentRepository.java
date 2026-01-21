package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.DebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {

    @Query("SELECT COALESCE(SUM(dp.interestPaid), 0) FROM DebtPayment dp " +
            "WHERE dp.debt = :debt AND dp.paymentDate BETWEEN :startDate AND :endDate AND dp.deleted = false")
    BigDecimal getInterestPaidByDebtAndDateRange(
            @Param("debt") Debt debt,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(dp.paymentAmount), 0) FROM DebtPayment dp " +
            "WHERE dp.debt.user.userId = :userId AND dp.paymentDate BETWEEN :startDate AND :endDate AND dp.deleted = false")
    BigDecimal getTotalPaymentsByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}