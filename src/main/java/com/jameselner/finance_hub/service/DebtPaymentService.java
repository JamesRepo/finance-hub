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
    public DebtPaymentDTO createDebtPaymentFromDto(final DebtPaymentDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getPaymentId() != null) {
            throw new IllegalArgumentException("Payment ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmounts(dto);

        DebtPayment debtPayment = debtPaymentMapper.toEntity(dto);
        DebtPayment savedPayment = debtPaymentRepository.save(debtPayment);

        updateDebtBalance(savedPayment.getDebt(), savedPayment.getPrincipalPaid());

        return debtPaymentMapper.toDto(savedPayment);
    }

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

    @Transactional
    public DebtPaymentDTO updateDebtPaymentFromDto(final DebtPaymentDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getPaymentId() == null) {
            throw new IllegalArgumentException("Payment ID must not be null for update operation");
        }

        DebtPayment existingPayment = debtPaymentRepository.findById(dto.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Debt payment with ID " + dto.getPaymentId() + " does not exist"));

        BigDecimal oldPrincipalPaid = existingPayment.getPrincipalPaid();

        validateDtoRequiredFields(dto);
        validateAmounts(dto);

        debtPaymentMapper.updateEntityFromDto(existingPayment, dto);
        DebtPayment savedPayment = debtPaymentRepository.save(existingPayment);

        BigDecimal principalDifference = savedPayment.getPrincipalPaid().subtract(oldPrincipalPaid);
        updateDebtBalance(savedPayment.getDebt(), principalDifference);

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

    public List<DebtPaymentDTO> findByDebtIdAsDto(final Long debtId) {
        if (debtId == null) {
            throw new IllegalArgumentException("Debt ID must not be null");
        }

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + debtId + " does not exist"));

        return debtPaymentRepository.findByDebtOrderByPaymentDateDesc(debt).stream()
                .map(debtPaymentMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<DebtPaymentDTO> findByDebtIdAndDateRangeAsDto(
            final Long debtId,
            final LocalDate startDate,
            final LocalDate endDate
    ) {
        if (debtId == null) {
            throw new IllegalArgumentException("Debt ID must not be null");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + debtId + " does not exist"));

        return debtPaymentRepository.findByDebtAndPaymentDateBetweenOrderByPaymentDateDesc(
                debt, startDate, endDate
        ).stream()
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

    public boolean existsById(final Long id) {
        validateIdNotNull(id);
        return debtPaymentRepository.existsById(id);
    }

    public long count() {
        return debtPaymentRepository.count();
    }

    public BigDecimal getTotalPaymentsByDebt(final Long debtId) {
        if (debtId == null) {
            throw new IllegalArgumentException("Debt ID must not be null");
        }

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + debtId + " does not exist"));

        return debtPaymentRepository.getTotalPaymentsByDebt(debt);
    }

    public BigDecimal getTotalPrincipalPaidByDebt(final Long debtId) {
        if (debtId == null) {
            throw new IllegalArgumentException("Debt ID must not be null");
        }

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + debtId + " does not exist"));

        return debtPaymentRepository.getTotalPrincipalPaidByDebt(debt);
    }

    public BigDecimal getTotalInterestPaidByDebt(final Long debtId) {
        if (debtId == null) {
            throw new IllegalArgumentException("Debt ID must not be null");
        }

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + debtId + " does not exist"));

        return debtPaymentRepository.getTotalInterestPaidByDebt(debt);
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

    private void validateDtoNotNull(final DebtPaymentDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DebtPaymentDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final DebtPaymentDTO dto) {
        if (dto.getDebtId() == null) {
            throw new IllegalArgumentException("Debt ID must not be null");
        }
        if (dto.getPaymentAmount() == null) {
            throw new IllegalArgumentException("Payment amount must not be null");
        }
        if (dto.getPrincipalPaid() == null) {
            throw new IllegalArgumentException("Principal paid must not be null");
        }
        if (dto.getInterestPaid() == null) {
            throw new IllegalArgumentException("Interest paid must not be null");
        }
        if (dto.getPaymentDate() == null) {
            throw new IllegalArgumentException("Payment date must not be null");
        }
    }

    private void validateAmounts(final DebtPaymentDTO dto) {
        if (dto.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (dto.getPrincipalPaid().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Principal paid must not be negative");
        }
        if (dto.getInterestPaid().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest paid must not be negative");
        }

        BigDecimal totalPaid = dto.getPrincipalPaid().add(dto.getInterestPaid());
        if (!totalPaid.equals(dto.getPaymentAmount())) {
            throw new IllegalArgumentException(
                    "Principal paid + Interest paid must equal payment amount");
        }
    }
}
