package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeSourceRepository extends JpaRepository<IncomeSource, Long> {

    List<IncomeSource> findByUserOrderByStartDateDesc(User user);

    List<IncomeSource> findByUserAndIsActiveOrderByStartDateDesc(User user, Boolean isActive);

    @Query("SELECT COUNT(i) FROM IncomeSource i WHERE i.user = :user AND i.isActive = true")
    long countActiveIncomeSourcesByUser(@Param("user") User user);

    @Query("SELECT COUNT(i) FROM IncomeSource i WHERE i.user = :user AND i.isActive = true AND i.isRecurring = true")
    long countRecurringIncomeSourcesByUser(@Param("user") User user);

    @Query("SELECT i FROM IncomeSource i " +
            "WHERE i.user = :user " +
            "AND i.startDate >= :startMonth " +
            "AND i.startDate <= :endMonth " +
            "ORDER BY i.startDate DESC")
    List<IncomeSource> findByUserAndMonthRange(
            @Param("user") User user,
            @Param("startMonth") LocalDate startMonth,
            @Param("endMonth") LocalDate endMonth
    );

    @Query("SELECT i FROM IncomeSource i " +
            "WHERE i.user = :user " +
            "AND i.isActive = true " +
            "AND i.startDate >= :startDate " +
            "AND i.startDate <= :endDate " +
            "ORDER BY i.startDate ASC")
    List<IncomeSource> findActiveInPeriod(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
