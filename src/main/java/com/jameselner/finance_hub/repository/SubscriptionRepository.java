package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Subscription;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.SubscriptionFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Find all subscriptions for a user
     */
    List<Subscription> findByUserOrderByPaymentDateDesc(User user);

    /**
     * Find subscriptions by frequency
     */
    List<Subscription> findByUserAndFrequencyOrderByPaymentDateDesc(User user, SubscriptionFrequency frequency);

    /**
     * Find subscriptions paid in a specific month
     */
    @Query("SELECT s FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND YEAR(s.paymentDate) = :year " +
           "AND MONTH(s.paymentDate) = :month " +
           "ORDER BY s.paymentDate DESC")
    List<Subscription> findByUserAndPaymentMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * Find subscriptions within a date range
     */
    @Query("SELECT s FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND s.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY s.paymentDate DESC")
    List<Subscription> findByUserAndPaymentDateBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate total for subscriptions within a date range
     */
    @Query("SELECT COALESCE(SUM(s.amount), 0) " +
           "FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND s.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalForPeriod(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate total for current month
     */
    @Query("SELECT COALESCE(SUM(s.amount), 0) " +
           "FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND YEAR(s.paymentDate) = :year " +
           "AND MONTH(s.paymentDate) = :month")
    BigDecimal calculateTotalForMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * Count subscriptions for a user
     */
    long countByUser(User user);

    /**
     * Count subscriptions in current month
     */
    @Query("SELECT COUNT(s) FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND YEAR(s.paymentDate) = :year " +
           "AND MONTH(s.paymentDate) = :month")
    long countByUserAndMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * Count subscriptions within a date range
     */
    @Query("SELECT COUNT(s) FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND s.paymentDate BETWEEN :startDate AND :endDate")
    long countByUserAndPaymentDateBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
