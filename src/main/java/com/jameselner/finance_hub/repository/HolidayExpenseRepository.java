package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Holiday;
import com.jameselner.finance_hub.domain.HolidayExpense;
import com.jameselner.finance_hub.domain.enums.HolidayExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface HolidayExpenseRepository extends JpaRepository<HolidayExpense, Long> {

    /**
     * Find all expenses for a holiday
     */
    List<HolidayExpense> findByHolidayOrderByExpenseDateDesc(Holiday holiday);

    /**
     * Find expenses by holiday and type
     */
    List<HolidayExpense> findByHolidayAndExpenseTypeOrderByExpenseDateDesc(
            Holiday holiday,
            HolidayExpenseType expenseType
    );

    /**
     * Count expenses for a holiday
     */
    long countByHoliday(Holiday holiday);

    /**
     * Calculate total spent for a holiday
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM HolidayExpense e WHERE e.holiday = :holiday")
    BigDecimal calculateTotalSpent(@Param("holiday") Holiday holiday);

    /**
     * Calculate total spent by expense type for a holiday
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM HolidayExpense e " +
           "WHERE e.holiday = :holiday AND e.expenseType = :expenseType")
    BigDecimal calculateTotalByType(
            @Param("holiday") Holiday holiday,
            @Param("expenseType") HolidayExpenseType expenseType
    );

    /**
     * Delete all expenses for a holiday
     */
    void deleteByHoliday(Holiday holiday);
}