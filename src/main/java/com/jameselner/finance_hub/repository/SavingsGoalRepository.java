package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.SavingsGoal;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserOrderByTargetDateAsc(User user);

    List<SavingsGoal> findByUserAndPriorityOrderByTargetDateAsc(User user, Priority priority);

    @Query("SELECT COALESCE(SUM(sg.targetAmount), 0) FROM SavingsGoal sg WHERE sg.user = :user")
    BigDecimal getTotalTargetAmountByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(sg.currentAmount), 0) FROM SavingsGoal sg WHERE sg.user = :user")
    BigDecimal getTotalCurrentSavingsAmountByUser(@Param("user") User user);

    @Query("SELECT sg FROM SavingsGoal sg WHERE sg.user = :user AND sg.currentAmount >= sg.targetAmount")
    List<SavingsGoal> findCompletedGoalsByUser(@Param("user") User user);

    @Query("SELECT sg FROM SavingsGoal sg WHERE sg.user = :user AND sg.currentAmount < sg.targetAmount")
    List<SavingsGoal> findActiveGoalsByUser(@Param("user") User user);
}
