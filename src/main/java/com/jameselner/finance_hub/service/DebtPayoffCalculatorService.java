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
        // Create working copies of balances and track interest paid per debt
        List<BigDecimal> balances = new ArrayList<>();
        List<BigDecimal> interestPaid = new ArrayList<>();
        List<BigDecimal> originalBalances = new ArrayList<>();
        List<Integer> monthsPaidOff = new ArrayList<>();

        for (Debt debt : sortedDebts) {
            balances.add(debt.getCurrentBalance());
            originalBalances.add(debt.getCurrentBalance());
            interestPaid.add(BigDecimal.ZERO);
            monthsPaidOff.add(0);
        }

        int months = 0;
        LocalDate currentDate = LocalDate.now();

        // Simulate month by month until all debts are paid off (max 100 years)
        while (months < 1200) {
            // Check if all debts are paid off
            boolean allPaidOff = balances.stream()
                    .allMatch(b -> b.compareTo(BigDecimal.ZERO) <= 0);
            if (allPaidOff) {
                break;
            }

            months++;
            BigDecimal remainingPayment = totalMonthlyPayment;

            // First pass: add interest to all debts with balance
            for (int i = 0; i < sortedDebts.size(); i++) {
                if (balances.get(i).compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal monthlyInterest = debtService.calculateMonthlyInterest(
                            balances.get(i), sortedDebts.get(i).getInterestRate());
                    balances.set(i, balances.get(i).add(monthlyInterest));
                    interestPaid.set(i, interestPaid.get(i).add(monthlyInterest));
                }
            }

            // Second pass: pay minimum payments on all debts
            for (int i = 0; i < sortedDebts.size(); i++) {
                if (balances.get(i).compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal minimum = sortedDebts.get(i).getMinimumPayment();
                    if (minimum == null) {
                        minimum = BigDecimal.ZERO;
                    }
                    BigDecimal payment = minimum.min(balances.get(i)).min(remainingPayment);
                    balances.set(i, balances.get(i).subtract(payment));
                    remainingPayment = remainingPayment.subtract(payment);

                    if (balances.get(i).compareTo(BigDecimal.ZERO) <= 0) {
                        balances.set(i, BigDecimal.ZERO);
                        if (monthsPaidOff.get(i) == 0) {
                            monthsPaidOff.set(i, months);
                        }
                    }
                }
            }

            // Third pass: apply extra payment to highest priority debt with balance
            for (int i = 0; i < sortedDebts.size() && remainingPayment.compareTo(BigDecimal.ZERO) > 0; i++) {
                if (balances.get(i).compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal extraPayment = remainingPayment.min(balances.get(i));
                    balances.set(i, balances.get(i).subtract(extraPayment));
                    remainingPayment = remainingPayment.subtract(extraPayment);

                    if (balances.get(i).compareTo(BigDecimal.ZERO) <= 0) {
                        balances.set(i, BigDecimal.ZERO);
                        if (monthsPaidOff.get(i) == 0) {
                            monthsPaidOff.set(i, months);
                        }
                    }
                }
            }
        }

        // Build projections from simulation results
        List<DebtPayoffProjection> projections = new ArrayList<>();
        for (int i = 0; i < sortedDebts.size(); i++) {
            Debt debt = sortedDebts.get(i);
            int debtMonths = monthsPaidOff.get(i) > 0 ? monthsPaidOff.get(i) : months;
            BigDecimal totalPaidForDebt = originalBalances.get(i).add(interestPaid.get(i));

            projections.add(DebtPayoffProjection.builder()
                    .debtId(debt.getDebtId())
                    .debtName(debt.getDebtName())
                    .currentBalance(originalBalances.get(i))
                    .interestRate(debt.getInterestRate())
                    .monthlyPayment(debt.getMinimumPayment() != null ? debt.getMinimumPayment() : BigDecimal.ZERO)
                    .monthsToPayoff(debtMonths)
                    .projectedPayoffDate(currentDate.plusMonths(debtMonths))
                    .totalInterestPaid(interestPaid.get(i))
                    .totalPaid(totalPaidForDebt)
                    .build());
        }

        int maxMonths = monthsPaidOff.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(months);

        LocalDate finalPayoffDate = currentDate.plusMonths(maxMonths);

        BigDecimal totalInterest = interestPaid.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = originalBalances.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(totalInterest);

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
