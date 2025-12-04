package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Budget;
import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Find all budgets for a specific user
     */
    List<Budget> findByUser(User user);

    /**
     * Find all budgets for a specific user ordered by start date descending
     */
    List<Budget> findByUserOrderByStartDateDesc(User user);

    /**
     * Find a budget by user and category
     */
    Optional<Budget> findByUserAndCategory(User user, Category category);

    /**
     * Find all active budgets for a user on a specific date
     */
    @Query("SELECT b FROM Budget b WHERE b.user = :user " +
           "AND :date BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveByUserAndDate(
            @Param("user") User user,
            @Param("date") LocalDate date
    );

    /**
     * Find budgets for a user within a date range
     */
    @Query("SELECT b FROM Budget b WHERE b.user = :user " +
           "AND (b.startDate <= :endDate AND b.endDate >= :startDate)")
    List<Budget> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find a budget by user, category, and overlapping dates
     */
    @Query("SELECT b FROM Budget b WHERE b.user = :user " +
           "AND b.category = :category " +
           "AND (b.startDate <= :endDate AND b.endDate >= :startDate)")
    List<Budget> findOverlappingBudgets(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Check if a user exists with the given ID
     */
    boolean existsByUserAndCategory(User user, Category category);
}
