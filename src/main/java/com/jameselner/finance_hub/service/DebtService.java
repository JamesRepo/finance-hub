package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.mapper.DebtMapper;
import com.jameselner.finance_hub.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtMapper debtMapper;

    @Transactional
    public DebtDTO createDebtFromDto(final DebtDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getDebtId() != null) {
            throw new IllegalArgumentException("Debt ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmounts(dto);

        Debt debt = debtMapper.toEntity(dto);
        Debt savedDebt = debtRepository.save(debt);
        return debtMapper.toDto(savedDebt);
    }

    @Transactional
    public DebtDTO updateDebtFromDto(final DebtDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getDebtId() == null) {
            throw new IllegalArgumentException("Debt ID must not be null for update operation");
        }

        Debt existingDebt = debtRepository.findById(dto.getDebtId())
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + dto.getDebtId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateAmounts(dto);

        debtMapper.updateEntityFromDto(existingDebt, dto);
        Debt savedDebt = debtRepository.save(existingDebt);
        return debtMapper.toDto(savedDebt);
    }

    public Optional<DebtDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return debtRepository.findById(id)
                .filter(debt -> !Boolean.TRUE.equals(debt.getDeleted()))
                .map(debtMapper::toDto);
    }

    public List<DebtDTO> findAllAsDto() {
        return debtRepository.findAllByDeletedFalse().stream()
                .map(debtMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Soft deleting debt with ID {}", id);

        Debt debt = debtRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + id + " does not exist"));

        debt.setDeleted(true);
        debtRepository.save(debt);
    }

    @Transactional
    public DebtDTO setActive(final Long id, final boolean active) {
        validateIdNotNull(id);

        Debt debt = debtRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + id + " does not exist"));

        debt.setActive(active);
        Debt savedDebt = debtRepository.save(debt);
        log.debug("Set debt {} active status to {}", id, active);
        return debtMapper.toDto(savedDebt);
    }

    public BigDecimal getTotalDebtByUser(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        return debtRepository.getTotalDebtByUser(user);
    }

    public BigDecimal calculateMonthlyInterest(final BigDecimal currentBalance, final BigDecimal annualRate) {
        if (currentBalance == null || annualRate == null) {
            throw new IllegalArgumentException("Balance and rate must not be null");
        }
        if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance must not be negative");
        }
        if (annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate must not be negative");
        }

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        return currentBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDailyInterest(final BigDecimal currentBalance, final BigDecimal annualRate) {
        if (currentBalance == null || annualRate == null) {
            throw new IllegalArgumentException("Balance and rate must not be null");
        }
        if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance must not be negative");
        }
        if (annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate must not be negative");
        }

        BigDecimal dailyRate = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

        return currentBalance.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateAccruedInterest(
            final BigDecimal currentBalance,
            final BigDecimal annualRate,
            final LocalDate startDate,
            final LocalDate endDate
    ) {
        if (currentBalance == null || annualRate == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
        if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance must not be negative");
        }
        if (annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate must not be negative");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal dailyInterest = calculateDailyInterest(currentBalance, annualRate);

        return dailyInterest.multiply(BigDecimal.valueOf(daysBetween)).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void applyInterestAccrual(final Long debtId, final LocalDate accrualDate) {
        validateIdNotNull(debtId);
        if (accrualDate == null) {
            throw new IllegalArgumentException("Accrual date must not be null");
        }

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt with ID " + debtId + " does not exist"));

        BigDecimal monthlyInterest = calculateMonthlyInterest(debt.getCurrentBalance(), debt.getInterestRate());
        BigDecimal newBalance = debt.getCurrentBalance().add(monthlyInterest);

        debt.setCurrentBalance(newBalance);
        debtRepository.save(debt);

        log.debug("Applied interest of {} to debt {}. New balance: {}",
                monthlyInterest, debtId, newBalance);
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Debt ID must not be null");
        }
    }

    private void validateDtoNotNull(final DebtDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DebtDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final DebtDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getDebtName() == null || dto.getDebtName().trim().isEmpty()) {
            throw new IllegalArgumentException("Debt name must not be null or empty");
        }
        if (dto.getDebtType() == null) {
            throw new IllegalArgumentException("Debt type must not be null");
        }
        if (dto.getCurrentBalance() == null) {
            throw new IllegalArgumentException("Current balance must not be null");
        }
        if (dto.getInterestRate() == null) {
            throw new IllegalArgumentException("Interest rate must not be null");
        }
    }

    private void validateAmounts(final DebtDTO dto) {
        if (dto.getPrincipalAmount() != null && dto.getPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal amount must be greater than zero");
        }
        if (dto.getCurrentBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current balance must not be negative");
        }
        if (dto.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate must not be negative");
        }
        if (dto.getMinimumPayment() != null && dto.getMinimumPayment().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum payment must not be negative");
        }
    }
}
