package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.HousingExpense;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.HousingExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HousingExpenseRepository extends JpaRepository<HousingExpense, Long> {

    /**
     * Find all housing expenses for a user ordered by month (newest first)
     */
    List<HousingExpense> findByUserOrderByExpenseMonthDesc(User user);

    /**
     * Find housing expenses for a specific month
     */
    List<HousingExpense> findByUserAndExpenseMonthOrderByExpenseTypeAsc(User user, LocalDate expenseMonth);

    /**
     * Find housing expenses by user and type
     */
    List<HousingExpense> findByUserAndExpenseTypeOrderByExpenseMonthDesc(User user, HousingExpenseType expenseType);

    /**
     * Find housing expenses within a date range (month range)
     */
    @Query("SELECT h FROM HousingExpense h " +
           "WHERE h.user = :user " +
           "AND h.expenseMonth >= :startMonth " +
           "AND h.expenseMonth <= :endMonth " +
           "ORDER BY h.expenseMonth DESC, h.expenseType ASC")
    List<HousingExpense> findByUserAndMonthRange(
            @Param("user") User user,
            @Param("startMonth") LocalDate startMonth,
            @Param("endMonth") LocalDate endMonth
    );

    /**
     * Count housing expenses for a user
     */
    long countByUser(User user);

    /**
     * Count housing expenses by user and type
     */
    long countByUserAndExpenseType(User user, HousingExpenseType expenseType);

    /**
     * Get total amount for a specific month
     */
    @Query("SELECT COALESCE(SUM(h.amount), 0) FROM HousingExpense h " +
           "WHERE h.user = :user AND h.expenseMonth = :month")
    BigDecimal getTotalForMonth(@Param("user") User user, @Param("month") LocalDate month);

    /**
     * Get distinct months that have expenses for a user
     */
    @Query("SELECT DISTINCT h.expenseMonth FROM HousingExpense h " +
           "WHERE h.user = :user " +
           "ORDER BY h.expenseMonth DESC")
    List<LocalDate> findDistinctMonthsByUser(@Param("user") User user);
}
