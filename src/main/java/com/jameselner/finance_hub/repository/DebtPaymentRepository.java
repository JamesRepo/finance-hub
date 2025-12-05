package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.DebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {

    List<DebtPayment> findByDebtOrderByPaymentDateDesc(Debt debt);

    List<DebtPayment> findByDebtAndPaymentDateBetweenOrderByPaymentDateDesc(
            Debt debt,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(dp.paymentAmount), 0) FROM DebtPayment dp WHERE dp.debt = :debt")
    BigDecimal getTotalPaymentsByDebt(@Param("debt") Debt debt);

    @Query("SELECT COALESCE(SUM(dp.principalPaid), 0) FROM DebtPayment dp WHERE dp.debt = :debt")
    BigDecimal getTotalPrincipalPaidByDebt(@Param("debt") Debt debt);

    @Query("SELECT COALESCE(SUM(dp.interestPaid), 0) FROM DebtPayment dp WHERE dp.debt = :debt")
    BigDecimal getTotalInterestPaidByDebt(@Param("debt") Debt debt);
}