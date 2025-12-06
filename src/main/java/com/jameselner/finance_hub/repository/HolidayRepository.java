package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Holiday;
import com.jameselner.finance_hub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * Find all holidays for a user
     */
    List<Holiday> findByUserOrderByStartDateDesc(User user);

    /**
     * Find active holidays for a user
     */
    List<Holiday> findByUserAndIsActiveTrueOrderByStartDateDesc(User user);

    /**
     * Find holidays within a date range
     */
    @Query("SELECT h FROM Holiday h " +
           "WHERE h.user = :user " +
           "AND h.startDate <= :endDate " +
           "AND h.endDate >= :startDate " +
           "ORDER BY h.startDate DESC")
    List<Holiday> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find upcoming holidays (start date is in the future)
     */
    @Query("SELECT h FROM Holiday h " +
           "WHERE h.user = :user " +
           "AND h.isActive = true " +
           "AND h.startDate > :currentDate " +
           "ORDER BY h.startDate ASC")
    List<Holiday> findUpcomingHolidays(
            @Param("user") User user,
            @Param("currentDate") LocalDate currentDate
    );

    /**
     * Find past holidays (end date is in the past)
     */
    @Query("SELECT h FROM Holiday h " +
           "WHERE h.user = :user " +
           "AND h.endDate < :currentDate " +
           "ORDER BY h.startDate DESC")
    List<Holiday> findPastHolidays(
            @Param("user") User user,
            @Param("currentDate") LocalDate currentDate
    );

    /**
     * Find current holidays (today is between start and end date)
     */
    @Query("SELECT h FROM Holiday h " +
           "WHERE h.user = :user " +
           "AND h.isActive = true " +
           "AND h.startDate <= :currentDate " +
           "AND h.endDate >= :currentDate " +
           "ORDER BY h.startDate ASC")
    List<Holiday> findCurrentHolidays(
            @Param("user") User user,
            @Param("currentDate") LocalDate currentDate
    );

    /**
     * Count active holidays for a user
     */
    long countByUserAndIsActiveTrue(User user);
}