package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.SavingsGoal;
import com.jameselner.finance_hub.domain.SavingsGoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SavingsGoalContributionRepository extends JpaRepository<SavingsGoalContribution, Long> {

    List<SavingsGoalContribution> findBySavingsGoalOrderByContributionDateDesc(SavingsGoal savingsGoal);

    List<SavingsGoalContribution> findBySavingsGoalAndContributionDateBetweenOrderByContributionDateDesc(
            SavingsGoal savingsGoal,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(sgc.amount), 0) FROM SavingsGoalContribution sgc WHERE sgc.savingsGoal = :goal")
    BigDecimal getTotalContributionsByGoal(@Param("goal") SavingsGoal goal);

    @Query("SELECT COUNT(sgc) FROM SavingsGoalContribution sgc WHERE sgc.savingsGoal = :goal")
    long getContributionCountByGoal(@Param("goal") SavingsGoal goal);
}
