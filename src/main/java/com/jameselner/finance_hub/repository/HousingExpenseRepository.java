package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.HousingExpense;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.HousingExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HousingExpenseRepository extends JpaRepository<HousingExpense, Long> {

    /**
     * Find all housing expenses for a user
     */
    List<HousingExpense> findByUserOrderByStartDateDesc(User user);

    /**
     * Find active housing expenses for a user
     */
    List<HousingExpense> findByUserAndIsActiveTrueOrderByStartDateDesc(User user);

    /**
     * Find housing expenses by user and type
     */
    List<HousingExpense> findByUserAndExpenseTypeOrderByStartDateDesc(User user, HousingExpenseType expenseType);

    /**
     * Find active housing expenses by user and type
     */
    List<HousingExpense> findByUserAndExpenseTypeAndIsActiveTrueOrderByStartDateDesc(
            User user,
            HousingExpenseType expenseType
    );

    /**
     * Find housing expenses within a date range
     * Includes expenses that are active during any part of the date range
     */
    @Query("SELECT h FROM HousingExpense h " +
           "WHERE h.user = :user " +
           "AND h.startDate <= :endDate " +
           "AND (h.endDate IS NULL OR h.endDate >= :startDate) " +
           "ORDER BY h.startDate DESC")
    List<HousingExpense> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find active housing expenses within a date range
     */
    @Query("SELECT h FROM HousingExpense h " +
           "WHERE h.user = :user " +
           "AND h.isActive = true " +
           "AND h.startDate <= :endDate " +
           "AND (h.endDate IS NULL OR h.endDate >= :startDate) " +
           "ORDER BY h.startDate DESC")
    List<HousingExpense> findActiveByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Count active housing expenses for a user
     */
    long countByUserAndIsActiveTrue(User user);

    /**
     * Count housing expenses by user and type
     */
    long countByUserAndExpenseType(User user, HousingExpenseType expenseType);
}
