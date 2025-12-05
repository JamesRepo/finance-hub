package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeSourceRepository extends JpaRepository<IncomeSource, Long> {

    List<IncomeSource> findByUserOrderByStartDateDesc(User user);

    List<IncomeSource> findByUserAndIsActiveOrderByStartDateDesc(User user, Boolean isActive);

    List<IncomeSource> findByUserAndIsRecurringOrderByStartDateDesc(User user, Boolean isRecurring);

    List<IncomeSource> findByUserAndIsActiveAndIsRecurringOrderByNextExpectedDateAsc(
            User user, Boolean isActive, Boolean isRecurring);

    @Query("SELECT i FROM IncomeSource i WHERE i.user = :user AND i.isActive = true " +
            "AND i.isRecurring = true AND i.nextExpectedDate <= :date " +
            "ORDER BY i.nextExpectedDate ASC")
    List<IncomeSource> findDueRecurringIncome(@Param("user") User user, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeSource i " +
            "WHERE i.user = :user AND i.isActive = true")
    BigDecimal getTotalActiveIncomeByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeSource i " +
            "WHERE i.user = :user AND i.isActive = true AND i.isRecurring = true")
    BigDecimal getTotalRecurringIncomeByUser(@Param("user") User user);

    @Query("SELECT COUNT(i) FROM IncomeSource i WHERE i.user = :user AND i.isActive = true")
    long countActiveIncomeSourcesByUser(@Param("user") User user);

    @Query("SELECT COUNT(i) FROM IncomeSource i WHERE i.user = :user AND i.isActive = true AND i.isRecurring = true")
    long countRecurringIncomeSourcesByUser(@Param("user") User user);
}
