package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.DashboardSummaryDto;
import com.jameselner.finance_hub.repository.AccountRepository;
import com.jameselner.finance_hub.repository.DebtRepository;
import com.jameselner.finance_hub.repository.TransactionRepository;
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

    public DashboardSummaryDto getDashboardSummary(final User user) {
        // Get the current month date range
        YearMonth currentMonth = YearMonth.now();
        LocalDate currentMonthStart = currentMonth.atDay(1);
        LocalDate currentMonthEnd = currentMonth.atEndOfMonth();

        // Get previous month date range
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate previousMonthStart = previousMonth.atDay(1);
        LocalDate previousMonthEnd = previousMonth.atEndOfMonth();

        // Calculate the total balance from all active accounts
        BigDecimal totalBalance = accountRepository.getTotalBalanceByUser(user);

        // Calculate monthly income (current month)
        BigDecimal monthlyIncome = transactionRepository.sumByUserAndTypeAndDateRange(
                user, TransactionType.INCOME, currentMonthStart, currentMonthEnd);

        // Calculate monthly expenses (current month)
        BigDecimal monthlyExpenses = transactionRepository.sumByUserAndTypeAndDateRange(
                user, TransactionType.EXPENSE, currentMonthStart, currentMonthEnd);

        // Calculate previous month income for comparison
        BigDecimal previousMonthIncome = transactionRepository.sumByUserAndTypeAndDateRange(
                user, TransactionType.INCOME, previousMonthStart, previousMonthEnd);

        // Calculate previous month expenses for comparison
        BigDecimal previousMonthExpenses = transactionRepository.sumByUserAndTypeAndDateRange(
                user, TransactionType.EXPENSE, previousMonthStart, previousMonthEnd);

        // Calculate total debt
        BigDecimal totalDebt = debtRepository.getTotalDebtByUser(user);

        return new DashboardSummaryDto(
                totalBalance,
                monthlyIncome,
                monthlyExpenses,
                totalDebt,
                previousMonthIncome,
                previousMonthExpenses,
                null
        );
    }
}
