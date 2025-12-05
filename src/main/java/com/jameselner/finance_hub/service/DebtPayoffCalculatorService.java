package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.dto.DebtPayoffPlan;
import com.jameselner.finance_hub.dto.DebtPayoffProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtPayoffCalculatorService {

    private final DebtService debtService;

    public DebtPayoffProjection calculatePayoffProjection(
            final Debt debt,
            final BigDecimal monthlyPayment
    ) {
        if (debt == null) {
            throw new IllegalArgumentException("Debt must not be null");
        }
        if (monthlyPayment == null || monthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly payment must be greater than zero");
        }

        BigDecimal balance = debt.getCurrentBalance();
        BigDecimal interestRate = debt.getInterestRate();
        BigDecimal totalInterestPaid = BigDecimal.ZERO;
        int months = 0;
        LocalDate currentDate = LocalDate.now();

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return DebtPayoffProjection.builder()
                    .debtId(debt.getDebtId())
                    .debtName(debt.getDebtName())
                    .currentBalance(balance)
                    .interestRate(interestRate)
                    .monthlyPayment(monthlyPayment)
                    .monthsToPayoff(0)
                    .projectedPayoffDate(currentDate)
                    .totalInterestPaid(BigDecimal.ZERO)
                    .totalPaid(BigDecimal.ZERO)
                    .build();
        }

        while (balance.compareTo(BigDecimal.ZERO) > 0 && months < 1200) {
            BigDecimal monthlyInterest = debtService.calculateMonthlyInterest(balance, interestRate);
            totalInterestPaid = totalInterestPaid.add(monthlyInterest);

            balance = balance.add(monthlyInterest);

            if (balance.compareTo(monthlyPayment) <= 0) {
                balance = BigDecimal.ZERO;
                months++;
                break;
            }

            balance = balance.subtract(monthlyPayment);
            months++;
        }

        LocalDate projectedPayoffDate = currentDate.plusMonths(months);
        BigDecimal totalPaid = debt.getCurrentBalance().add(totalInterestPaid);

        return DebtPayoffProjection.builder()
                .debtId(debt.getDebtId())
                .debtName(debt.getDebtName())
                .currentBalance(debt.getCurrentBalance())
                .interestRate(interestRate)
                .monthlyPayment(monthlyPayment)
                .monthsToPayoff(months)
                .projectedPayoffDate(projectedPayoffDate)
                .totalInterestPaid(totalInterestPaid)
                .totalPaid(totalPaid)
                .build();
    }

    public DebtPayoffPlan calculateAvalancheMethod(
            final List<Debt> debts,
            final BigDecimal totalMonthlyPayment
    ) {
        if (debts == null || debts.isEmpty()) {
            throw new IllegalArgumentException("Debts list must not be null or empty");
        }
        if (totalMonthlyPayment == null || totalMonthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total monthly payment must be greater than zero");
        }

        List<Debt> sortedDebts = debts.stream()
                .sorted(Comparator.comparing(Debt::getInterestRate).reversed())
                .collect(Collectors.toList());

        return calculatePayoffPlan(sortedDebts, totalMonthlyPayment, "Avalanche (Highest Interest First)");
    }

    public DebtPayoffPlan calculateSnowballMethod(
            final List<Debt> debts,
            final BigDecimal totalMonthlyPayment
    ) {
        if (debts == null || debts.isEmpty()) {
            throw new IllegalArgumentException("Debts list must not be null or empty");
        }
        if (totalMonthlyPayment == null || totalMonthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total monthly payment must be greater than zero");
        }

        List<Debt> sortedDebts = debts.stream()
                .sorted(Comparator.comparing(Debt::getCurrentBalance))
                .collect(Collectors.toList());

        return calculatePayoffPlan(sortedDebts, totalMonthlyPayment, "Snowball (Lowest Balance First)");
    }

    private DebtPayoffPlan calculatePayoffPlan(
            final List<Debt> sortedDebts,
            final BigDecimal totalMonthlyPayment,
            final String strategyName
    ) {
        List<DebtPayoffProjection> projections = new ArrayList<>();
        BigDecimal remainingPayment = totalMonthlyPayment;

        BigDecimal totalMinimumPayment = sortedDebts.stream()
                .map(Debt::getMinimumPayment)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalMonthlyPayment.compareTo(totalMinimumPayment) < 0) {
            throw new IllegalArgumentException(
                    "Total monthly payment must be at least the sum of all minimum payments: " + totalMinimumPayment);
        }

        BigDecimal extraPayment = totalMonthlyPayment.subtract(totalMinimumPayment);

        for (int i = 0; i < sortedDebts.size(); i++) {
            Debt debt = sortedDebts.get(i);
            BigDecimal minimumPayment = debt.getMinimumPayment() != null ?
                    debt.getMinimumPayment() : BigDecimal.ZERO;

            BigDecimal paymentForThisDebt;
            if (i == 0) {
                paymentForThisDebt = minimumPayment.add(extraPayment);
            } else {
                paymentForThisDebt = minimumPayment;
            }

            DebtPayoffProjection projection = calculatePayoffProjection(debt, paymentForThisDebt);
            projections.add(projection);

            if (i < sortedDebts.size() - 1) {
                extraPayment = extraPayment.add(minimumPayment);
            }
        }

        int maxMonths = projections.stream()
                .mapToInt(DebtPayoffProjection::getMonthsToPayoff)
                .max()
                .orElse(0);

        LocalDate finalPayoffDate = projections.stream()
                .map(DebtPayoffProjection::getProjectedPayoffDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        BigDecimal totalInterest = projections.stream()
                .map(DebtPayoffProjection::getTotalInterestPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = projections.stream()
                .map(DebtPayoffProjection::getTotalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DebtPayoffPlan.builder()
                .strategyName(strategyName)
                .totalMonthlyPayment(totalMonthlyPayment)
                .totalMonthsToPayoff(maxMonths)
                .finalPayoffDate(finalPayoffDate)
                .totalInterestPaid(totalInterest)
                .totalAmountPaid(totalPaid)
                .debtProjections(projections)
                .build();
    }

    public int calculateMonthsToPayoff(
            final BigDecimal currentBalance,
            final BigDecimal interestRate,
            final BigDecimal monthlyPayment
    ) {
        if (currentBalance == null || interestRate == null || monthlyPayment == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
        if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (monthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly payment must be greater than zero");
        }

        BigDecimal balance = currentBalance;
        int months = 0;

        while (balance.compareTo(BigDecimal.ZERO) > 0 && months < 1200) {
            BigDecimal monthlyInterest = balance
                    .multiply(interestRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);

            balance = balance.add(monthlyInterest);

            if (balance.compareTo(monthlyPayment) <= 0) {
                return months + 1;
            }

            balance = balance.subtract(monthlyPayment);
            months++;
        }

        return months;
    }
}
