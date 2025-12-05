package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.IncomeDeduction;
import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.enums.DeductionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface IncomeDeductionRepository extends JpaRepository<IncomeDeduction, Long> {

    List<IncomeDeduction> findByIncomeSourceOrderByDeductionTypeAsc(IncomeSource incomeSource);

    List<IncomeDeduction> findByIncomeSourceAndIsActiveOrderByDeductionTypeAsc(
            IncomeSource incomeSource, Boolean isActive);

    List<IncomeDeduction> findByIncomeSourceAndDeductionType(
            IncomeSource incomeSource, DeductionType deductionType);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM IncomeDeduction d " +
            "WHERE d.incomeSource = :incomeSource AND d.isActive = true")
    BigDecimal getTotalDeductionsByIncomeSource(@Param("incomeSource") IncomeSource incomeSource);

    @Query("SELECT COUNT(d) FROM IncomeDeduction d " +
            "WHERE d.incomeSource = :incomeSource AND d.isActive = true")
    long countActiveDeductionsByIncomeSource(@Param("incomeSource") IncomeSource incomeSource);
}
