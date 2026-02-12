package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.SavingsGoal;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.Priority;
import com.jameselner.finance_hub.dto.SavingsGoalDTO;
import com.jameselner.finance_hub.dto.SavingsGoalProgressDTO;
import com.jameselner.finance_hub.mapper.SavingsGoalMapper;
import com.jameselner.finance_hub.repository.SavingsGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final SavingsGoalMapper savingsGoalMapper;

    @Transactional
    public SavingsGoalDTO createSavingsGoalFromDto(final SavingsGoalDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getGoalId() != null) {
            throw new IllegalArgumentException("Goal ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmounts(dto);

        if (dto.getCurrentAmount() == null) {
            dto.setCurrentAmount(BigDecimal.ZERO);
        }

        SavingsGoal savingsGoal = savingsGoalMapper.toEntity(dto);
        SavingsGoal savedGoal = savingsGoalRepository.save(savingsGoal);
        return savingsGoalMapper.toDto(savedGoal);
    }

    @Transactional
    public SavingsGoalDTO updateSavingsGoalFromDto(final SavingsGoalDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getGoalId() == null) {
            throw new IllegalArgumentException("Goal ID must not be null for update operation");
        }

        SavingsGoal existingGoal = savingsGoalRepository.findById(dto.getGoalId())
                .orElseThrow(() -> new IllegalArgumentException("Savings goal with ID " + dto.getGoalId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateAmounts(dto);

        savingsGoalMapper.updateEntityFromDto(existingGoal, dto);
        SavingsGoal savedGoal = savingsGoalRepository.save(existingGoal);
        return savingsGoalMapper.toDto(savedGoal);
    }

    public Optional<SavingsGoalDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return savingsGoalRepository.findById(id)
                .map(savingsGoalMapper::toDto);
    }

    public List<SavingsGoalDTO> findAllAsDto() {
        return savingsGoalRepository.findAll().stream()
                .map(savingsGoalMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SavingsGoalDTO> findByUserAsDto(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        return savingsGoalRepository.findByUserOrderByTargetDateAsc(user).stream()
                .map(savingsGoalMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SavingsGoalDTO> findByUserAndPriorityAsDto(final User user, final Priority priority) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority must not be null");
        }
        return savingsGoalRepository.findByUserAndPriorityOrderByTargetDateAsc(user, priority).stream()
                .map(savingsGoalMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SavingsGoalDTO> findActiveGoalsByUserAsDto(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        return savingsGoalRepository.findActiveGoalsByUser(user).stream()
                .map(savingsGoalMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SavingsGoalDTO> findCompletedGoalsByUserAsDto(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        return savingsGoalRepository.findCompletedGoalsByUser(user).stream()
                .map(savingsGoalMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting savings goal with ID {}", id);

        if (!savingsGoalRepository.existsById(id)) {
            throw new IllegalArgumentException("Savings goal with ID " + id + " does not exist");
        }

        savingsGoalRepository.deleteById(id);
    }

    public BigDecimal getTotalTargetAmountByUser(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        return savingsGoalRepository.getTotalTargetAmountByUser(user);
    }

    public BigDecimal getTotalCurrentAmountByUser(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        return savingsGoalRepository.getTotalCurrentSavingsAmountByUser(user);
    }

    public SavingsGoalProgressDTO calculateProgress(final Long goalId) {
        validateIdNotNull(goalId);

        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Savings goal with ID " + goalId + " does not exist"));

        return calculateProgress(goal);
    }

    public SavingsGoalProgressDTO calculateProgress(final SavingsGoal goal) {
        if (goal == null) {
            throw new IllegalArgumentException("Savings goal must not be null");
        }

        BigDecimal targetAmount = goal.getTargetAmount();
        BigDecimal currentAmount = goal.getCurrentAmount();
        BigDecimal remainingAmount = targetAmount.subtract(currentAmount);

        BigDecimal percentageComplete = BigDecimal.ZERO;
        if (targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            percentageComplete = currentAmount.divide(targetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        Long daysRemaining = null;
        BigDecimal recommendedMonthlyContribution = null;
        BigDecimal recommendedWeeklyContribution = null;
        boolean onTrack = false;
        String status;

        if (goal.getTargetDate() != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());

            if (daysRemaining > 0) {
                BigDecimal monthsRemaining = BigDecimal.valueOf(daysRemaining)
                        .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);

                if (monthsRemaining.compareTo(BigDecimal.ZERO) > 0) {
                    recommendedMonthlyContribution = remainingAmount
                            .divide(monthsRemaining, 2, RoundingMode.HALF_UP);
                }

                BigDecimal weeksRemaining = BigDecimal.valueOf(daysRemaining)
                        .divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

                if (weeksRemaining.compareTo(BigDecimal.ZERO) > 0) {
                    recommendedWeeklyContribution = remainingAmount
                            .divide(weeksRemaining, 2, RoundingMode.HALF_UP);
                }

                BigDecimal expectedProgress = BigDecimal.valueOf(
                        ChronoUnit.DAYS.between(goal.getCreatedAt().toLocalDate(), LocalDate.now())
                ).divide(BigDecimal.valueOf(
                        ChronoUnit.DAYS.between(goal.getCreatedAt().toLocalDate(), goal.getTargetDate())
                ), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

                onTrack = percentageComplete.compareTo(expectedProgress) >= 0;
            }
        }

        if (currentAmount.compareTo(targetAmount) >= 0) {
            status = "COMPLETED";
        } else if (daysRemaining != null && daysRemaining < 0) {
            status = "OVERDUE";
        } else if (onTrack) {
            status = "ON_TRACK";
        } else {
            status = "BEHIND";
        }

        return SavingsGoalProgressDTO.builder()
                .goalId(goal.getGoalId())
                .goalName(goal.getGoalName())
                .targetAmount(targetAmount)
                .currentAmount(currentAmount)
                .remainingAmount(remainingAmount)
                .percentageComplete(percentageComplete)
                .targetDate(goal.getTargetDate())
                .daysRemaining(daysRemaining)
                .recommendedMonthlyContribution(recommendedMonthlyContribution)
                .recommendedWeeklyContribution(recommendedWeeklyContribution)
                .onTrack(onTrack)
                .status(status)
                .build();
    }

    public List<SavingsGoalProgressDTO> calculateProgressForAllGoals(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        return savingsGoalRepository.findByUserOrderByTargetDateAsc(user).stream()
                .map(this::calculateProgress)
                .collect(Collectors.toList());
    }

    public BigDecimal calculateTotalRecommendedMonthlyContribution(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        return savingsGoalRepository.findActiveGoalsByUser(user).stream()
                .map(this::calculateProgress)
                .map(SavingsGoalProgressDTO::getRecommendedMonthlyContribution)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void addContribution(final Long goalId, final BigDecimal amount) {
        validateIdNotNull(goalId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Contribution amount must be greater than zero");
        }

        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Savings goal with ID " + goalId + " does not exist"));

        BigDecimal newAmount = goal.getCurrentAmount().add(amount);
        goal.setCurrentAmount(newAmount);

        savingsGoalRepository.save(goal);

        log.debug("Added contribution of {} to goal {}. New amount: {}", amount, goalId, newAmount);
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Savings goal ID must not be null");
        }
    }

    private void validateDtoNotNull(final SavingsGoalDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("SavingsGoalDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final SavingsGoalDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getGoalName() == null || dto.getGoalName().trim().isEmpty()) {
            throw new IllegalArgumentException("Goal name must not be null or empty");
        }
        if (dto.getTargetAmount() == null) {
            throw new IllegalArgumentException("Target amount must not be null");
        }
    }

    private void validateAmounts(final SavingsGoalDTO dto) {
        if (dto.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target amount must be greater than zero");
        }
        if (dto.getCurrentAmount() != null && dto.getCurrentAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current amount must not be negative");
        }
    }
}
