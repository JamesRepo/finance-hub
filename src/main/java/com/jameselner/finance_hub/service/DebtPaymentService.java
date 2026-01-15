package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.DebtPayment;
import com.jameselner.finance_hub.dto.DebtPaymentDTO;
import com.jameselner.finance_hub.mapper.DebtPaymentMapper;
import com.jameselner.finance_hub.repository.DebtPaymentRepository;
import com.jameselner.finance_hub.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DebtPaymentService {

    private final DebtPaymentRepository debtPaymentRepository;
    private final DebtRepository debtRepository;
    private final DebtPaymentMapper debtPaymentMapper;
    private final DebtService debtService;

    @Transactional
    public DebtPaymentDTO recordPaymentWithInterestCalculation(
            final Long debtId,
            final BigDecimal paymentAmount,
            final LocalDate paymentDate
    ) {
        if (debtId == null) {
            throw new IllegalArgumentException("Debt ID must not be null");
        }
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date must not be null");
        }

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + debtId + " does not exist"));

        BigDecimal currentBalance = debt.getCurrentBalance();
        if (paymentAmount.compareTo(currentBalance) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount cannot exceed current balance: " + currentBalance);
        }

        BigDecimal monthlyInterest = debtService.calculateMonthlyInterest(
                currentBalance,
                debt.getInterestRate()
        );

        BigDecimal principalPaid;
        BigDecimal interestPaid;

        if (paymentAmount.compareTo(monthlyInterest) <= 0) {
            interestPaid = paymentAmount;
            principalPaid = BigDecimal.ZERO;
        } else {
            interestPaid = monthlyInterest;
            principalPaid = paymentAmount.subtract(monthlyInterest);
        }

        DebtPayment debtPayment = new DebtPayment();
        debtPayment.setDebt(debt);
        debtPayment.setPaymentAmount(paymentAmount);
        debtPayment.setPrincipalPaid(principalPaid);
        debtPayment.setInterestPaid(interestPaid);
        debtPayment.setPaymentDate(paymentDate);

        DebtPayment savedPayment = debtPaymentRepository.save(debtPayment);

        updateDebtBalance(debt, principalPaid);

        log.debug("Recorded payment of {} for debt {}. Principal: {}, Interest: {}",
                paymentAmount, debtId, principalPaid, interestPaid);

        return debtPaymentMapper.toDto(savedPayment);
    }

    public Optional<DebtPaymentDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return debtPaymentRepository.findById(id)
                .map(debtPaymentMapper::toDto);
    }

    public List<DebtPaymentDTO> findAllAsDto() {
        return debtPaymentRepository.findAll().stream()
                .map(debtPaymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting debt payment with ID {}", id);

        DebtPayment payment = debtPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Debt payment with ID " + id + " does not exist"));

        BigDecimal principalToReverse = payment.getPrincipalPaid().negate();
        updateDebtBalance(payment.getDebt(), principalToReverse);

        debtPaymentRepository.deleteById(id);
    }


    public BigDecimal getLastMonthInterestPaidByDebt(final Long debtId) {
        if (debtId == null) {
            throw new IllegalArgumentException("Debt ID must not be null");
        }

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + debtId + " does not exist"));

        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = lastMonth.atDay(1);
        LocalDate endDate = lastMonth.atEndOfMonth();

        return debtPaymentRepository.getInterestPaidByDebtAndDateRange(debt, startDate, endDate);
    }

    /**
     * Calculate total debt payments for a user for a specific month
     */
    public BigDecimal calculateTotalForMonth(final Long userId, final LocalDate month) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (month == null) {
            throw new IllegalArgumentException("Month must not be null");
        }
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = YearMonth.from(month).atEndOfMonth();
        return debtPaymentRepository.getTotalPaymentsByUserAndDateRange(userId, startDate, endDate);
    }

    /**
     * Calculate total debt payments for a user for a specific year
     */
    public BigDecimal calculateTotalForYear(final Long userId, final int year) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return debtPaymentRepository.getTotalPaymentsByUserAndDateRange(userId, startDate, endDate);
    }

    private void updateDebtBalance(final Debt debt, final BigDecimal principalPaid) {
        BigDecimal newBalance = debt.getCurrentBalance().subtract(principalPaid);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }

        debt.setCurrentBalance(newBalance);
        debtRepository.save(debt);

        log.debug("Updated debt {} balance to {}", debt.getDebtId(), newBalance);
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Debt payment ID must not be null");
        }
    }
}
