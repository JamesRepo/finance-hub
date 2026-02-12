package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.DashboardSummaryDto;
import com.jameselner.finance_hub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DebtRepository debtRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final SavingsGoalRepository savingsGoalRepository;

    public DashboardSummaryDto getDashboardSummary(final User user) {
        // Get the current month date range
        YearMonth currentMonth = YearMonth.now();
        LocalDate currentMonthStart = currentMonth.atDay(1);
        LocalDate currentMonthEnd = currentMonth.atEndOfMonth();

        // Calculate the total balance from all active accounts
        BigDecimal totalBalance = accountRepository.getTotalBalanceByUser(user);

        // Calculate last monthly income (last time was paid)
        IncomeSource lastMonthlyIncomeSource = incomeSourceRepository.getLastMonthlyIncomeByUser(user);
        BigDecimal lastMonthlyIncome = BigDecimal.ZERO;
        if (lastMonthlyIncomeSource != null && lastMonthlyIncomeSource.getNetAmount() != null) {
            lastMonthlyIncome = lastMonthlyIncomeSource.getNetAmount();
        }

        // Calculate monthly expenses (current month)
        BigDecimal monthlyExpenses = transactionRepository.sumByUserAndTypeAndDateRange(
                user, TransactionType.EXPENSE, currentMonthStart, currentMonthEnd);

        // Calculate total debt
        BigDecimal totalDebt = debtRepository.getTotalDebtByUser(user);

        // Calculate total savings
        BigDecimal totalSavings = savingsGoalRepository.getTotalCurrentSavingsAmountByUser(user);

        return new DashboardSummaryDto(
                totalBalance,
                lastMonthlyIncome,
                monthlyExpenses,
                totalDebt,
                totalSavings
        );
    }
}
