package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.SavingsGoal;
import com.jameselner.finance_hub.domain.SavingsGoalContribution;
import com.jameselner.finance_hub.dto.SavingsGoalContributionDTO;
import com.jameselner.finance_hub.mapper.SavingsGoalContributionMapper;
import com.jameselner.finance_hub.repository.SavingsGoalContributionRepository;
import com.jameselner.finance_hub.repository.SavingsGoalRepository;
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
public class SavingsGoalContributionService {

    private final SavingsGoalContributionRepository contributionRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final SavingsGoalContributionMapper contributionMapper;
    private final SavingsGoalService savingsGoalService;

    @Transactional
    public SavingsGoalContributionDTO createContributionFromDto(final SavingsGoalContributionDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getContributionId() != null) {
            throw new IllegalArgumentException("Contribution ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmount(dto);

        SavingsGoalContribution contribution = contributionMapper.toEntity(dto);
        SavingsGoalContribution savedContribution = contributionRepository.save(contribution);

        savingsGoalService.addContribution(dto.getGoalId(), dto.getAmount());

        return contributionMapper.toDto(savedContribution);
    }

    @Transactional
    public SavingsGoalContributionDTO recordContribution(
            final Long goalId,
            final BigDecimal amount,
            final LocalDate contributionDate,
            final String note
    ) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID must not be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Contribution amount must be greater than zero");
        }
        if (contributionDate == null) {
            throw new IllegalArgumentException("Contribution date must not be null");
        }

        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Savings goal with ID " + goalId + " does not exist"));

        SavingsGoalContribution contribution = new SavingsGoalContribution();
        contribution.setSavingsGoal(goal);
        contribution.setAmount(amount);
        contribution.setContributionDate(contributionDate);
        contribution.setNote(note);

        SavingsGoalContribution savedContribution = contributionRepository.save(contribution);

        savingsGoalService.addContribution(goalId, amount);

        log.debug("Recorded contribution of {} for goal {}", amount, goalId);

        return contributionMapper.toDto(savedContribution);
    }

    @Transactional
    public SavingsGoalContributionDTO updateContributionFromDto(final SavingsGoalContributionDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getContributionId() == null) {
            throw new IllegalArgumentException("Contribution ID must not be null for update operation");
        }

        SavingsGoalContribution existingContribution = contributionRepository.findById(dto.getContributionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contribution with ID " + dto.getContributionId() + " does not exist"));

        BigDecimal oldAmount = existingContribution.getAmount();

        validateDtoRequiredFields(dto);
        validateAmount(dto);

        contributionMapper.updateEntityFromDto(existingContribution, dto);
        SavingsGoalContribution savedContribution = contributionRepository.save(existingContribution);

        BigDecimal amountDifference = dto.getAmount().subtract(oldAmount);
        if (amountDifference.compareTo(BigDecimal.ZERO) != 0) {
            savingsGoalService.addContribution(dto.getGoalId(), amountDifference);
        }

        return contributionMapper.toDto(savedContribution);
    }

    public Optional<SavingsGoalContributionDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return contributionRepository.findById(id)
                .map(contributionMapper::toDto);
    }

    public List<SavingsGoalContributionDTO> findAllAsDto() {
        return contributionRepository.findAll().stream()
                .map(contributionMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SavingsGoalContributionDTO> findByGoalIdAsDto(final Long goalId) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID must not be null");
        }

        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Savings goal with ID " + goalId + " does not exist"));

        return contributionRepository.findBySavingsGoalOrderByContributionDateDesc(goal).stream()
                .map(contributionMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SavingsGoalContributionDTO> findByGoalIdAndDateRangeAsDto(
            final Long goalId,
            final LocalDate startDate,
            final LocalDate endDate
    ) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID must not be null");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Savings goal with ID " + goalId + " does not exist"));

        return contributionRepository.findBySavingsGoalAndContributionDateBetweenOrderByContributionDateDesc(
                goal, startDate, endDate
        ).stream()
                .map(contributionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting contribution with ID {}", id);

        SavingsGoalContribution contribution = contributionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contribution with ID " + id + " does not exist"));

        BigDecimal amountToReverse = contribution.getAmount().negate();
        savingsGoalService.addContribution(contribution.getSavingsGoal().getGoalId(), amountToReverse);

        contributionRepository.deleteById(id);
    }

    public BigDecimal getTotalContributionsByGoal(final Long goalId) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID must not be null");
        }

        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Savings goal with ID " + goalId + " does not exist"));

        return contributionRepository.getTotalContributionsByGoal(goal);
    }

    public long getContributionCountByGoal(final Long goalId) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID must not be null");
        }

        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Savings goal with ID " + goalId + " does not exist"));

        return contributionRepository.getContributionCountByGoal(goal);
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Contribution ID must not be null");
        }
    }

    private void validateDtoNotNull(final SavingsGoalContributionDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("SavingsGoalContributionDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final SavingsGoalContributionDTO dto) {
        if (dto.getGoalId() == null) {
            throw new IllegalArgumentException("Goal ID must not be null");
        }
        if (dto.getAmount() == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (dto.getContributionDate() == null) {
            throw new IllegalArgumentException("Contribution date must not be null");
        }
    }

    private void validateAmount(final SavingsGoalContributionDTO dto) {
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
